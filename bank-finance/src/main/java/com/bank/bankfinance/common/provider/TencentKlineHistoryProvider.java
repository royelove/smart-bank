package com.bank.bankfinance.common.provider;

import com.bank.bankfinance.common.constant.KlinePeriod;
import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.entity.StockKline;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 腾讯历史 K 线（web.ifzq.gtimg.cn，非官方接口）。
 *
 * <p>接口形态（已由第三方实现交叉验证：{@code cnstock-cli} 的
 * {@code KlineEndpoint = https://web.ifzq.gtimg.cn/appstock/app/%s/get?param=%s"} 与
 * {@code KlineBar{ Date, Open, Close, High, Low, Volume }}）：
 * <pre>
 *   https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=sh600519,day,,,320,qfq
 *   {"code":0,"msg":"","data":{"sh600519":{"qfqday":[["2024-01-02","1685.00","1685.01","1699.00","1670.00","23000.00"], ...]}}}
 * </pre>
 * 每根柱子字段顺序为 {@code [时间, 开, 收, 高, 低, 量]}；分钟线走 {@code kline/mkline}，
 * 时间格式为 {@code yyyyMMddHHmm}。
 *
 * <p><b>解析刻意写得宽松</b>：不绑定 {@code qfqday / day / qfqweek / m5} 这类具体字段名，
 * 而是在 {@code data.<symbol>} 下取第一个「元素是长度 ≥ 6 的数组」的数组。
 * 上游改名时最坏结果是「抓不到 → 回落到本地数据」，而不是解析异常。
 *
 * <p>本类所有失败路径都返回空列表，绝不向上抛异常：
 * 它只是「把真实历史喂给 K 线」的锦上添花，不能影响图表可用性。
 */
@Component
public class TencentKlineHistoryProvider {

    private static final Logger log = LoggerFactory.getLogger(TencentKlineHistoryProvider.class);

    private static final String BASE = "https://web.ifzq.gtimg.cn/appstock/app/";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    /** 腾讯日线/周线/月线的时间戳只带日期，分钟线带到分钟 */
    private static final DateTimeFormatter COMPACT_DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 抓取真实历史 K 线。任何异常/空数据都返回空列表。
     *
     * @param limit 期望条数（腾讯最多约 320 根/次）
     */
    public List<StockKline> fetch(Stock stock, KlinePeriod period, int limit) {
        if (stock == null || stock.getStockCode() == null || period == null) {
            return Collections.emptyList();
        }
        String symbol = toSymbol(stock.getStockCode(), stock.getMarket());
        String url = buildUrl(symbol, period, limit);
        if (url == null) {
            return Collections.emptyList();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "smart-bank/1.0")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("腾讯K线请求失败 code={} url={}", response.statusCode(), url);
                return Collections.emptyList();
            }
            String body = new String(response.body(), StandardCharsets.UTF_8);
            return parse(stock, period, symbol, body);
        } catch (Exception e) {
            // 网络不可达 / 超时 / 上游改格式，一律降级
            log.warn("腾讯K线抓取失败, stock={}, period={}: {}", stock.getStockCode(), period.getCode(), e.toString());
            return Collections.emptyList();
        }
    }

    private String buildUrl(String symbol, KlinePeriod period, int limit) {
        int n = Math.max(10, Math.min(limit, 320));
        return switch (period) {
            case DAY1 -> BASE + "fqkline/get?param=" + symbol + ",day,,," + n + ",qfq";
            case WEEK1 -> BASE + "fqkline/get?param=" + symbol + ",week,,," + n + ",qfq";
            case MONTH1 -> BASE + "fqkline/get?param=" + symbol + ",month,,," + n + ",qfq";
            case MIN1 -> BASE + "kline/mkline?param=" + symbol + ",m1,," + n;
            case MIN5 -> BASE + "kline/mkline?param=" + symbol + ",m5,," + n;
        };
    }

    private String toSymbol(String stockCode, String market) {
        if ("沪".equals(market) || stockCode.startsWith("6")) {
            return "sh" + stockCode;
        }
        if (stockCode.startsWith("4") || stockCode.startsWith("8")) {
            return "bj" + stockCode;
        }
        return "sz" + stockCode;
    }

    private List<StockKline> parse(Stock stock, KlinePeriod period, String symbol, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode symbolNode = root.path("data").path(symbol);
            if (!symbolNode.isObject()) {
                return Collections.emptyList();
            }
            JsonNode rows = firstBarArray(symbolNode);
            if (rows == null) {
                return Collections.emptyList();
            }
            List<StockKline> bars = new ArrayList<>();
            for (JsonNode row : rows) {
                StockKline bar = toBar(stock, period, row);
                if (bar != null) {
                    bars.add(bar);
                }
            }
            bars.sort((a, b) -> a.getKlineTime().compareTo(b.getKlineTime()));
            return bars;
        } catch (Exception e) {
            log.warn("腾讯K线解析失败, stock={}, period={}: {}", stock.getStockCode(), period.getCode(), e.toString());
            return Collections.emptyList();
        }
    }

    /** 在 data.<symbol> 下找第一个「元素为长度≥6数组」的数组，避开 qt 之类的元数据字段 */
    private JsonNode firstBarArray(JsonNode symbolNode) {
        for (Iterator<Map.Entry<String, JsonNode>> it = symbolNode.fields(); it.hasNext(); ) {
            JsonNode candidate = it.next().getValue();
            if (!candidate.isArray() || candidate.isEmpty()) {
                continue;
            }
            JsonNode first = candidate.get(0);
            if (first.isArray() && first.size() >= 6) {
                return candidate;
            }
        }
        return null;
    }

    private StockKline toBar(Stock stock, KlinePeriod period, JsonNode row) {
        if (row == null || row.size() < 6) {
            return null;
        }
        LocalDateTime time = parseTime(period, row.get(0).asText(null));
        if (time == null) {
            return null;
        }
        BigDecimal open = decimal(row.get(1));
        BigDecimal close = decimal(row.get(2));
        BigDecimal high = decimal(row.get(3));
        BigDecimal low = decimal(row.get(4));
        if (open == null || close == null || high == null || low == null) {
            return null;
        }
        StockKline bar = new StockKline();
        bar.setStockCode(stock.getStockCode());
        bar.setStockName(stock.getStockName());
        bar.setPeriod(period.getCode());
        bar.setKlineTime(period.startOf(time));
        bar.setOpenPrice(open);
        bar.setClosePrice(close);
        bar.setHighPrice(high);
        bar.setLowPrice(low);
        bar.setVolume(decimal(row.get(5)) == null ? 0L : decimal(row.get(5)).longValue());
        bar.setAmount(close.multiply(BigDecimal.valueOf(bar.getVolume() == null ? 0L : bar.getVolume())));
        bar.setDataSource("remote");
        return bar;
    }

    private LocalDateTime parseTime(KlinePeriod period, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim();
        try {
            if (period.isIntraday()) {
                // yyyyMMddHHmm（个别情况下只给 HHmm）
                if (s.length() >= 12) {
                    return LocalDateTime.parse(s.substring(0, 12), MINUTE_FMT);
                }
                return LocalDate.now().atTime(LocalTime.parse(s.substring(0, 4), DateTimeFormatter.ofPattern("HHmm")));
            }
            if (s.length() >= 10) {
                return LocalDate.parse(s.substring(0, 10), DAY_FMT).atStartOfDay();
            }
            return LocalDate.parse(s.substring(0, 8), COMPACT_DAY_FMT).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal decimal(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            String text = node.asText("").trim();
            if (text.isEmpty()) {
                return null;
            }
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }
}
