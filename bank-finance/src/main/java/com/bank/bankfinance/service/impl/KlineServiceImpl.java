package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.constant.KlinePeriod;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.provider.TencentKlineHistoryProvider;
import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.entity.StockKline;
import com.bank.bankfinance.entity.StockTrade;
import com.bank.bankfinance.mapper.StockKlineMapper;
import com.bank.bankfinance.mapper.StockMapper;
import com.bank.bankfinance.mapper.StockTradeMapper;
import com.bank.bankfinance.service.KlineService;
import com.bank.bankfinance.service.StockService;
import com.bank.bankfinance.vo.StockKlineVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.bank.bankfinance.common.constant.ResponseCode.STOCK_CODE_INVALID;
import static com.bank.bankfinance.common.constant.ResponseCode.STOCK_KLINE_PERIOD_INVALID;

/**
 * K 线实现。
 *
 * <p><b>实时性怎么来的</b>：一根柱子由「已落库的历史」+「本次请求时叠加的实时数据」组成，
 * 三步合成，越靠后优先级越高：
 * <ol>
 *   <li><b>历史</b>：库里已有的柱子；不足时优先抓腾讯真实历史，抓不到再按
 *       确定性算法补齐（同一根柱子每次算出来都一样，所以图表不会在刷新时跳变）。</li>
 *   <li><b>真实成交</b>：把 {@code t_stock_trade} 里本系统撮合产生的成交按周期聚合后覆盖上去，
 *       所以你在交易页真正成交的单子会立刻出现在 K 线里（{@code source=trade}）。</li>
 *   <li><b>实时行情</b>：当前这根<b>还没走完</b>的柱子，其收盘/最高/最低跟着 {@code t_stock.currentPrice}
 *       走，每次请求都会重算（{@code source=quote}，前端标 {@code live=true}）。</li>
 * </ol>
 * 另有 {@link #refreshCurrentBars()} 定时把当前柱落库，这样即使没人请求，柱子在后台也是持续推进的。
 *
 * <p><b>为什么不把模拟补齐的数据当真实数据展示</b>：每根柱子都带 {@code source} 字段
 * （{@code remote} 真实历史 / {@code trade} 真实成交 / {@code quote} 实时行情 / {@code simulated} 演示补齐），
 * 前端会据此显示"演示数据"标记，避免看的人误以为那是真实历史行情。
 */
@Service
public class KlineServiceImpl implements KlineService {

    private static final Logger log = LoggerFactory.getLogger(KlineServiceImpl.class);

    /** 每个「股票+周期」一把锁：补齐历史/续写当前柱都是 check-then-insert，必须串行 */
    private final ConcurrentHashMap<String, Object> writeLocks = new ConcurrentHashMap<>();
    /** 远端历史抓取失败后的冷却截止时间（毫秒时间戳），避免每次请求都去撞一个不可达的接口 */
    private final ConcurrentHashMap<String, Long> remoteFailedUntil = new ConcurrentHashMap<>();
    /** 定时刷新重入保护：fixedDelay 到点就触发，若上一轮还没跑完（行情源超时）直接跳过本轮 */
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private StockKlineMapper stockKlineMapper;

    @Autowired
    private StockTradeMapper stockTradeMapper;

    @Autowired
    private StockService stockService;

    @Autowired
    private TencentKlineHistoryProvider remoteHistoryProvider;

    @Value("${stock.kline.default-limit:120}")
    private int defaultLimit;

    @Value("${stock.kline.max-limit:500}")
    private int maxLimit;

    /** 历史不足时是否按确定性算法补齐（关掉则图表可能很空） */
    @Value("${stock.kline.seed-history:true}")
    private boolean seedHistory;

    /** 是否尝试抓取腾讯真实历史 K 线（失败自动降级，不影响可用性） */
    @Value("${stock.kline.remote-history:true}")
    private boolean remoteHistory;

    /** 远端抓取失败后的冷却时间（秒） */
    @Value("${stock.kline.remote-retry-seconds:600}")
    private long remoteRetrySeconds;

    /** 聚合真实成交时最多往回看多少天 */
    @Value("${stock.kline.trade-lookback-days:30}")
    private int tradeLookbackDays;

    /** 定时刷新当前柱的总开关 */
    @Value("${stock.kline.refresh-enabled:true}")
    private boolean refreshEnabled;

    @Override
    public List<StockKlineVO> getKline(String stockCode, String period, Integer limit) {
        String code = stockCode == null ? "" : stockCode.trim();
        KlinePeriod p = KlinePeriod.of(period == null || period.isBlank() ? "1day" : period);
        if (p == null) {
            throw new BusinessException(STOCK_KLINE_PERIOD_INVALID);
        }
        int n = normalizeLimit(limit);
        Stock stock = stockMapper.selectOne(new LambdaQueryWrapper<Stock>().eq(Stock::getStockCode, code));
        if (stock == null) {
            throw new BusinessException(STOCK_CODE_INVALID);
        }
        synchronized (lockOf(stock.getStockCode(), p)) {
            return buildSeries(stock, p, n);
        }
    }

    /** 组装「历史 + 真实成交 + 实时行情」 */
    private List<StockKlineVO> buildSeries(Stock stock, KlinePeriod p, int n) {
        LocalDateTime currentBucket = p.startOf(LocalDateTime.now());

        // 需要展示的 n 个连续周期起点（升序）
        List<LocalDateTime> buckets = new ArrayList<>(n);
        LocalDateTime cursor = currentBucket;
        for (int i = 0; i < n; i++) {
            buckets.add(cursor);
            cursor = p.previous(cursor);
        }
        Collections.reverse(buckets);

        LocalDateTime from = buckets.get(0);
        Map<LocalDateTime, StockKline> byTime = loadPersisted(stock.getStockCode(), p, from);

        fetchRemoteIfNeeded(stock, p, n, buckets, byTime);
        if (seedHistory) {
            // 当前这根柱子交给实时行情/真实成交去生成，不参与模拟补齐
            seedMissing(stock, p, buckets, currentBucket, byTime);
        }
        overlayTrades(stock, p, from, currentBucket, byTime);
        touchCurrentBar(stock, p, currentBucket, byTime);

        List<StockKlineVO> series = new ArrayList<>(buckets.size());
        for (LocalDateTime bucket : buckets) {
            StockKline bar = byTime.get(bucket);
            if (bar == null) {
                continue;
            }
            series.add(toVO(bar, bucket.equals(currentBucket)));
        }
        return series;
    }

    private Map<LocalDateTime, StockKline> loadPersisted(String stockCode, KlinePeriod p, LocalDateTime from) {
        List<StockKline> rows = stockKlineMapper.selectList(new LambdaQueryWrapper<StockKline>()
                .eq(StockKline::getStockCode, stockCode)
                .eq(StockKline::getPeriod, p.getCode())
                .ge(StockKline::getKlineTime, from)
                .orderByAsc(StockKline::getKlineTime));
        Map<LocalDateTime, StockKline> byTime = new HashMap<>();
        for (StockKline row : rows) {
            // 兼容旧数据里可能存在的重复柱：同一时间点取第一条
            byTime.putIfAbsent(row.getKlineTime(), row);
        }
        return byTime;
    }

    /** 库里历史明显不够时，尝试抓一次腾讯真实历史；失败则进入冷却，不影响后续请求 */
    private void fetchRemoteIfNeeded(Stock stock, KlinePeriod p, int n,
                                     List<LocalDateTime> buckets, Map<LocalDateTime, StockKline> byTime) {
        if (!remoteHistory) {
            return;
        }
        long missing = buckets.stream().filter(b -> !byTime.containsKey(b)).count();
        if (missing < Math.max(1, n / 2)) {
            return;
        }
        String key = stock.getStockCode() + '|' + p.getCode();
        Long failedUntil = remoteFailedUntil.get(key);
        if (failedUntil != null && System.currentTimeMillis() < failedUntil) {
            return;
        }
        List<StockKline> remote = remoteHistoryProvider.fetch(stock, p, Math.max(n, defaultLimit));
        if (remote.isEmpty()) {
            remoteFailedUntil.put(key, System.currentTimeMillis() + Math.max(60, remoteRetrySeconds) * 1000L);
            return;
        }
        remoteFailedUntil.remove(key);
        int inserted = 0;
        for (StockKline bar : remote) {
            if (bar.getKlineTime() == null || byTime.containsKey(bar.getKlineTime())) {
                continue;
            }
            byTime.put(bar.getKlineTime(), bar);
            if (insertQuietly(bar)) {
                inserted++;
            }
        }
        log.info("已载入腾讯历史K线 stock={} period={} 条数={}", stock.getStockCode(), p.getCode(), inserted);
    }

    /**
     * 按确定性算法补齐缺失的历史柱。
     *
     * <p>同一根柱子（股票+周期+时间）用同一个种子生成，因此无论什么时候、请求多少次，
     * 算出来的 OHLC 完全相同 —— 这是「图表刷新时不会整体跳变」的前提。
     */
    private void seedMissing(Stock stock, KlinePeriod p, List<LocalDateTime> buckets,
                             LocalDateTime currentBucket, Map<LocalDateTime, StockKline> byTime) {
        BigDecimal base = basePrice(stock);
        BigDecimal previousClose = null;
        for (LocalDateTime bucket : buckets) {
            StockKline existing = byTime.get(bucket);
            if (existing != null) {
                BigDecimal close = validPrice(existing.getClosePrice());
                if (close != null) {
                    previousClose = close;
                }
                continue;
            }
            if (bucket.equals(currentBucket)) {
                // 当前柱留给实时行情，这里只把"上一根收盘"传递下去
                continue;
            }
            BigDecimal open = previousClose != null ? previousClose : anchorPrice(stock, p, bucket, base);
            StockKline bar = synthesize(stock, p, bucket, open, base);
            byTime.put(bucket, bar);
            insertQuietly(bar);
            previousClose = bar.getClosePrice();
        }
    }

    /** 把本系统真实撮合成交按周期聚合成 OHLC，覆盖到对应柱子上（只在内存里合成，每次请求重算） */
    private void overlayTrades(Stock stock, KlinePeriod p, LocalDateTime from, LocalDateTime currentBucket,
                               Map<LocalDateTime, StockKline> byTime) {
        LocalDateTime floor = LocalDateTime.now().minusDays(Math.max(1, tradeLookbackDays));
        LocalDateTime start = from.isAfter(floor) ? from : floor;
        List<StockTrade> trades = stockTradeMapper.selectList(new LambdaQueryWrapper<StockTrade>()
                .eq(StockTrade::getStockCode, stock.getStockCode())
                .ge(StockTrade::getTradeTime, start)
                .orderByAsc(StockTrade::getTradeTime));
        if (trades.isEmpty()) {
            return;
        }
        Map<LocalDateTime, List<StockTrade>> grouped = new LinkedHashMap<>();
        for (StockTrade trade : trades) {
            if (trade.getTradeTime() == null || priceOf(trade) == null) {
                continue;
            }
            LocalDateTime bucket = p.startOf(trade.getTradeTime());
            if (bucket.isBefore(from) || bucket.isAfter(currentBucket)) {
                continue;
            }
            grouped.computeIfAbsent(bucket, k -> new ArrayList<>()).add(trade);
        }
        for (Map.Entry<LocalDateTime, List<StockTrade>> entry : grouped.entrySet()) {
            List<StockTrade> list = entry.getValue();
            BigDecimal open = priceOf(list.get(0));
            BigDecimal close = priceOf(list.get(list.size() - 1));
            if (open == null || close == null) {
                continue;
            }
            BigDecimal high = open;
            BigDecimal low = open;
            long lots = 0;
            BigDecimal amount = BigDecimal.ZERO;
            for (StockTrade trade : list) {
                BigDecimal price = priceOf(trade);
                if (price == null) {
                    continue;
                }
                high = high.max(price);
                low = low.min(price);
                if (trade.getTradeQuantity() != null) {
                    // 成交量统一用「手」：撮合里的数量是股
                    lots += trade.getTradeQuantity() / 100;
                }
                if (trade.getTradeAmount() != null) {
                    amount = amount.add(trade.getTradeAmount());
                }
            }
            StockKline bar = byTime.get(entry.getKey());
            if (bar == null) {
                bar = newBar(stock, p, entry.getKey());
                byTime.put(entry.getKey(), bar);
            }
            bar.setOpenPrice(open);
            bar.setClosePrice(close);
            bar.setHighPrice(high.max(open).max(close));
            bar.setLowPrice(low.min(open).min(close));
            bar.setVolume(lots);
            bar.setAmount(amount);
            bar.setDataSource("trade");
        }
    }

    /**
     * 当前柱跟随最新价：这就是图表「实时」的部分。
     *
     * <p>只在内存里改，不落库 —— 读接口不该写数据库；落库由定时任务
     * {@link #refreshCurrentBars()} 负责，否则每个客户端每次轮询都会产生一次 UPDATE。
     */
    private void touchCurrentBar(Stock stock, KlinePeriod p, LocalDateTime currentBucket,
                                 Map<LocalDateTime, StockKline> byTime) {
        BigDecimal price = validPrice(stock.getCurrentPrice());
        if (price == null) {
            price = validPrice(stock.getPrevClose());
        }
        if (price == null) {
            return;
        }
        StockKline bar = byTime.get(currentBucket);
        if (bar == null) {
            bar = newBar(stock, p, currentBucket);
            BigDecimal open = validPrice(stock.getPrevClose()) != null ? stock.getPrevClose() : price;
            bar.setOpenPrice(open);
            bar.setHighPrice(price.max(open));
            bar.setLowPrice(price.min(open));
            bar.setClosePrice(price);
            bar.setVolume(0L);
            bar.setAmount(BigDecimal.ZERO);
            bar.setDataSource("quote");
            byTime.put(currentBucket, bar);
            return;
        }
        if (validPrice(bar.getOpenPrice()) == null) {
            bar.setOpenPrice(price);
        }
        if (bar.getHighPrice() == null || price.compareTo(bar.getHighPrice()) > 0) {
            bar.setHighPrice(price);
        }
        if (bar.getLowPrice() == null || price.compareTo(bar.getLowPrice()) < 0) {
            bar.setLowPrice(price);
        }
        bar.setClosePrice(price);
        // 该柱若已含真实成交，保留 trade 标记（说明"这根柱子有真实成交"），否则视为行情合成
        if (bar.getDataSource() == null || "simulated".equals(bar.getDataSource())) {
            bar.setDataSource("quote");
        }
    }

    /**
     * 定时把当前柱落库，使 K 线在无人访问时也持续推进。
     *
     * <p>先拉一次行情刷新 {@code t_stock} 最新价；行情源不可用（例如外网不通）时只打日志，
     * 继续用库内已有最新价推进当前柱，不会伪造价格。
     */
    @Scheduled(fixedDelayString = "${stock.kline.refresh-interval-ms:10000}",
            initialDelayString = "${stock.kline.refresh-initial-delay-ms:20000}")
    public void refreshCurrentBars() {
        if (!refreshEnabled) {
            return;
        }
        if (!refreshing.compareAndSet(false, true)) {
            return;
        }
        try {
            try {
                stockService.listQuotes();
            } catch (Exception e) {
                log.warn("K线定时刷新：行情获取失败，沿用库内最新价推进当前柱 - {}", e.toString());
            }
            List<Stock> stocks = stockMapper.selectList(
                    new LambdaQueryWrapper<Stock>().eq(Stock::getStatus, 1));
            int updated = 0;
            for (Stock stock : stocks) {
                for (KlinePeriod p : KlinePeriod.values()) {
                    try {
                        synchronized (lockOf(stock.getStockCode(), p)) {
                            if (persistCurrentBar(stock, p)) {
                                updated++;
                            }
                        }
                    } catch (Exception e) {
                        log.debug("K线当前柱刷新失败 stock={} period={} - {}",
                                stock.getStockCode(), p.getCode(), e.toString());
                    }
                }
            }
            log.debug("K线定时刷新完成，更新当前柱 {} 根", updated);
        } finally {
            refreshing.set(false);
        }
    }

    /** 把某只股票某个周期的当前柱写入数据库（不存在则新建，开盘价取上一根收盘价） */
    private boolean persistCurrentBar(Stock stock, KlinePeriod p) {
        BigDecimal price = validPrice(stock.getCurrentPrice());
        if (price == null) {
            price = validPrice(stock.getPrevClose());
        }
        if (price == null) {
            return false;
        }
        LocalDateTime bucket = p.startOf(LocalDateTime.now());
        StockKline bar = findBar(stock.getStockCode(), p, bucket);
        if (bar == null) {
            BigDecimal open = null;
            StockKline previous = findBar(stock.getStockCode(), p, p.previous(bucket));
            if (previous != null) {
                open = validPrice(previous.getClosePrice());
            }
            if (open == null) {
                open = validPrice(stock.getPrevClose()) != null ? stock.getPrevClose() : price;
            }
            StockKline fresh = newBar(stock, p, bucket);
            fresh.setOpenPrice(open);
            fresh.setClosePrice(price);
            fresh.setHighPrice(price.max(open));
            fresh.setLowPrice(price.min(open));
            fresh.setVolume(0L);
            fresh.setAmount(BigDecimal.ZERO);
            fresh.setDataSource("quote");
            return insertQuietly(fresh);
        }
        boolean changed = false;
        if (bar.getHighPrice() == null || price.compareTo(bar.getHighPrice()) > 0) {
            bar.setHighPrice(price);
            changed = true;
        }
        if (bar.getLowPrice() == null || price.compareTo(bar.getLowPrice()) < 0) {
            bar.setLowPrice(price);
            changed = true;
        }
        if (bar.getClosePrice() == null || price.compareTo(bar.getClosePrice()) != 0) {
            bar.setClosePrice(price);
            changed = true;
        }
        if (validPrice(bar.getOpenPrice()) == null) {
            bar.setOpenPrice(price);
            changed = true;
        }
        if (!changed) {
            return false;
        }
        if (bar.getDataSource() == null || "simulated".equals(bar.getDataSource())) {
            bar.setDataSource("quote");
        }
        try {
            stockKlineMapper.updateById(bar);
            return true;
        } catch (Exception e) {
            log.debug("K线当前柱落库失败 {} {} - {}", bar.getStockCode(), bar.getPeriod(), e.toString());
            return false;
        }
    }

    private StockKline findBar(String stockCode, KlinePeriod p, LocalDateTime bucket) {
        List<StockKline> rows = stockKlineMapper.selectList(new LambdaQueryWrapper<StockKline>()
                .eq(StockKline::getStockCode, stockCode)
                .eq(StockKline::getPeriod, p.getCode())
                .eq(StockKline::getKlineTime, bucket)
                .last("LIMIT 1"));
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 落库；撞唯一索引说明并发/上一轮定时任务已写入，直接忽略 */
    private boolean insertQuietly(StockKline bar) {
        try {
            stockKlineMapper.insert(bar);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        } catch (Exception e) {
            log.debug("K线落库失败 {} {} {} - {}",
                    bar.getStockCode(), bar.getPeriod(), bar.getKlineTime(), e.toString());
            return false;
        }
    }

    private StockKline newBar(Stock stock, KlinePeriod p, LocalDateTime bucket) {
        StockKline bar = new StockKline();
        bar.setStockCode(stock.getStockCode());
        bar.setStockName(stock.getStockName());
        bar.setPeriod(p.getCode());
        bar.setKlineTime(bucket);
        return bar;
    }

    private StockKline synthesize(Stock stock, KlinePeriod p, LocalDateTime bucket,
                                 BigDecimal open, BigDecimal base) {
        Random random = new Random(seed(stock.getStockCode(), p.getCode(), bucket) * 31 + 7);
        double drift = (random.nextDouble() - 0.5) * volatility(p);
        BigDecimal close = clamp(scale(open.multiply(BigDecimal.valueOf(1 + drift))), base);
        BigDecimal high = scale(open.max(close).multiply(BigDecimal.valueOf(1 + random.nextDouble() * 0.005)));
        BigDecimal low = scale(open.min(close).multiply(BigDecimal.valueOf(1 - random.nextDouble() * 0.005)));
        long volume = synthVolume(p, random);

        StockKline bar = newBar(stock, p, bucket);
        bar.setOpenPrice(open);
        bar.setClosePrice(close);
        bar.setHighPrice(high.max(open).max(close));
        bar.setLowPrice(low.min(open).min(close));
        bar.setVolume(volume);
        bar.setAmount(close.multiply(BigDecimal.valueOf(volume)));
        bar.setDataSource("simulated");
        return bar;
    }

    /** 缺口的起点价：围绕现价做确定性扰动，保证补出来的历史量级合理 */
    private BigDecimal anchorPrice(Stock stock, KlinePeriod p, LocalDateTime bucket, BigDecimal base) {
        Random random = new Random(seed(stock.getStockCode(), p.getCode(), bucket));
        double factor = 1 + (random.nextDouble() - 0.5) * 0.06;
        return scale(base.multiply(BigDecimal.valueOf(factor)));
    }

    private double volatility(KlinePeriod p) {
        return switch (p) {
            case MIN1, MIN5 -> 0.004;
            case DAY1 -> 0.02;
            case WEEK1 -> 0.035;
            case MONTH1 -> 0.06;
        };
    }

    private long synthVolume(KlinePeriod p, Random random) {
        long floor = switch (p) {
            case MIN1, MIN5 -> 200L;
            case DAY1 -> 50_000L;
            case WEEK1 -> 250_000L;
            case MONTH1 -> 1_000_000L;
        };
        long span = switch (p) {
            case MIN1, MIN5 -> 3_000L;
            case DAY1 -> 450_000L;
            case WEEK1 -> 2_000_000L;
            case MONTH1 -> 8_000_000L;
        };
        return floor + (long) (random.nextDouble() * span);
    }

    private StockKlineVO toVO(StockKline bar, boolean live) {
        StockKlineVO vo = new StockKlineVO();
        vo.setStockCode(bar.getStockCode());
        vo.setStockName(bar.getStockName());
        vo.setPeriod(bar.getPeriod());
        vo.setOpenPrice(bar.getOpenPrice());
        vo.setClosePrice(bar.getClosePrice());
        vo.setHighPrice(bar.getHighPrice());
        vo.setLowPrice(bar.getLowPrice());
        vo.setVolume(bar.getVolume());
        vo.setAmount(bar.getAmount());
        vo.setKlineTime(bar.getKlineTime());
        vo.setSource(bar.getDataSource() == null ? "simulated" : bar.getDataSource());
        vo.setLive(live);
        return vo;
    }

    private Object lockOf(String stockCode, KlinePeriod p) {
        return writeLocks.computeIfAbsent(stockCode + '|' + p.getCode(), k -> new Object());
    }

    private int normalizeLimit(Integer limit) {
        int n = limit == null || limit <= 0 ? defaultLimit : limit;
        int cap = Math.max(10, maxLimit);
        return Math.max(10, Math.min(n, cap));
    }

    private BigDecimal basePrice(Stock stock) {
        BigDecimal price = validPrice(stock.getCurrentPrice());
        if (price == null) {
            price = validPrice(stock.getPrevClose());
        }
        return price != null ? price : BigDecimal.ONE;
    }

    private BigDecimal validPrice(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : null;
    }

    private BigDecimal priceOf(StockTrade trade) {
        return validPrice(trade.getTradePrice());
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** 把补齐出来的价格限制在现价的 ±25% 内，避免随机游走漂移到不合理的量级 */
    private BigDecimal clamp(BigDecimal value, BigDecimal base) {
        BigDecimal low = base.multiply(BigDecimal.valueOf(0.75));
        BigDecimal high = base.multiply(BigDecimal.valueOf(1.25));
        if (value.compareTo(low) < 0) {
            return scale(low);
        }
        if (value.compareTo(high) > 0) {
            return scale(high);
        }
        return value;
    }

    /** 由「股票+周期+周期起点」推出稳定种子：同一根柱子在任何时刻生成结果都一致 */
    private long seed(String stockCode, String period, LocalDateTime bucket) {
        long hash = 1125899906842597L;
        String key = stockCode + '|' + period + '|' + bucket;
        for (int i = 0; i < key.length(); i++) {
            hash = 31 * hash + key.charAt(i);
        }
        return hash;
    }
}
