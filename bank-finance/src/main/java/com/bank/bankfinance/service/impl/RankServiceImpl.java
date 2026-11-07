package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.entity.Stock;
import com.bank.bankfinance.entity.StockRank;
import com.bank.bankfinance.mapper.StockMapper;
import com.bank.bankfinance.mapper.StockRankMapper;
import com.bank.bankfinance.service.RankService;
import com.bank.bankfinance.event.RankUpdatedEvent;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RankServiceImpl implements RankService {

    private static final String RANK_PREFIX = "rank:";

    @Autowired private StockMapper stockMapper;
    @Autowired private StockRankMapper stockRankMapper;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private ApplicationEventPublisher eventPublisher;
    //写一个构造函数，只做赋值操作
    @Autowired
    RankServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    private ZSetOperations<String, Object> zSetOps;
    @PostConstruct
    public void init(){
        this.zSetOps = redisTemplate.opsForZSet();
    }
    @Override
    public List<StockRank> getRankList(String rankType, int topN) {
        String key = RANK_PREFIX + rankType;
        Set<ZSetOperations.TypedTuple<Object>> tuples = zSetOps.reverseRangeWithScores(key, 0, topN - 1);
        if (tuples == null || tuples.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Stock> stockMap = loadStockMap();
        List<StockRank> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            String stockCode = (String) tuple.getValue();
            Stock stock = stockMap.get(stockCode);
            if (stock == null) continue;
            StockRank sr = new StockRank();
            sr.setStockCode(stock.getStockCode());
            sr.setStockName(stock.getStockName());
            sr.setMarket(stock.getMarket());
            sr.setRankType(rankType);
            Double score = tuple.getScore();
            sr.setRankValue(score != null ? BigDecimal.valueOf(score) : BigDecimal.ZERO);
            sr.setCurrentPrice(stock.getCurrentPrice());
            sr.setPrevClose(stock.getPrevClose());
            sr.setHighPrice(stock.getHighPrice());
            sr.setLowPrice(stock.getLowPrice());
            sr.setVolume(stock.getVolume());
            sr.setAmount(stock.getAmount());
            sr.setTurnoverRate(stock.getTurnoverRate());
            sr.setRank(rank++);
            sr.setSnapshotTime(LocalDateTime.now());
            result.add(sr);
        }
        return result;
    }

    @Override
    public StockRank getRankDetail(String stockCode, String rankType) {
        String key = RANK_PREFIX + rankType;
        Double score = zSetOps.score(key, stockCode);
        if (score == null) return null;
        Long rank = zSetOps.reverseRank(key, stockCode);
        Stock stock = stockMapper.selectOne(
            new LambdaQueryWrapper<Stock>().eq(Stock::getStockCode, stockCode)
        );
        if (stock == null) return null;
        StockRank sr = new StockRank();
        sr.setStockCode(stock.getStockCode());
        sr.setStockName(stock.getStockName());
        sr.setMarket(stock.getMarket());
        sr.setRankType(rankType);
        sr.setRankValue(BigDecimal.valueOf(score));
        sr.setRank(rank != null ? (int)(rank + 1) : null);
        sr.setRankValue(score != null ? BigDecimal.valueOf(score) : BigDecimal.ZERO);
        sr.setPrevClose(stock.getPrevClose());
        sr.setHighPrice(stock.getHighPrice());
        sr.setLowPrice(stock.getLowPrice());
        sr.setVolume(stock.getVolume());
        sr.setAmount(stock.getAmount());
        sr.setTurnoverRate(stock.getTurnoverRate());
        sr.setSnapshotTime(LocalDateTime.now());
        return sr;
    }

    @Override
    public void refreshAllRank() {
        List<Stock> stocks = stockMapper.selectList(null);
        if (stocks.isEmpty()) return;

        String[] types = {"change", "fall", "volume", "amount", "turnover", "amplitude"};
        for (String type : types) {
            String key = RANK_PREFIX + type;
            zSetOps.removeRange(key, 0, Integer.MAX_VALUE);
        }

        for (Stock stock : stocks) {
            zSetOps.add(RANK_PREFIX + "change", stock.getStockCode(), computeChange(stock));
            zSetOps.add(RANK_PREFIX + "fall", stock.getStockCode(), computeFall(stock));
            zSetOps.add(RANK_PREFIX + "volume", stock.getStockCode(), stock.getVolume() != null ? stock.getVolume().doubleValue() : 0.0);
            zSetOps.add(RANK_PREFIX + "amount", stock.getStockCode(), stock.getAmount() != null ? stock.getAmount().doubleValue() : 0.0);
            zSetOps.add(RANK_PREFIX + "turnover", stock.getStockCode(), computeTurnover(stock));
            zSetOps.add(RANK_PREFIX + "amplitude", stock.getStockCode(), computeAmplitude(stock));
        }

        saveRankSnapshot(stocks);
        eventPublisher.publishEvent(new RankUpdatedEvent("all", null));
    }

    @Override
    public Map<String, List<StockRank>> getAllRankTopN(int topN) {
        Map<String, List<StockRank>> result = new LinkedHashMap<>();
        String[] types = {"change", "fall", "volume", "amount", "turnover", "amplitude"};
        for (String type : types) {
            result.put(type, getRankList(type, topN));
        }
        return result;
    }

    @Override
    public Integer getStockRank(String stockCode, String rankType) {
        String key = RANK_PREFIX + rankType;
        Long rank = zSetOps.reverseRank(key, stockCode);
        return rank != null ? (int)(rank + 1) : null;
    }

    @Override
    public Double getRankScore(String stockCode, String rankType) {
        return zSetOps.score(RANK_PREFIX + rankType, stockCode);
    }

    private double computeChange(Stock stock) {
        if (stock.getPrevClose() == null || stock.getPrevClose().signum() <= 0) return 0;
        if (stock.getCurrentPrice() == null) return 0;
        return stock.getCurrentPrice().subtract(stock.getPrevClose())
                .multiply(BigDecimal.valueOf(100))
                .divide(stock.getPrevClose(), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double computeFall(Stock stock) {
        return -computeChange(stock);
    }

    private double computeTurnover(Stock stock) {
        if (stock.getVolume() == null || stock.getVolume() <= 0) return 0;
        if (stock.getTotalShares() == null || stock.getTotalShares().signum() <= 0) return 0;
        return BigDecimal.valueOf(stock.getVolume()).multiply(BigDecimal.valueOf(100))
                .divide(stock.getTotalShares(), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private double computeAmplitude(Stock stock) {
        if (stock.getHighPrice() == null || stock.getLowPrice() == null || stock.getPrevClose() == null) return 0;
        if (stock.getPrevClose().signum() <= 0) return 0;
        return stock.getHighPrice().subtract(stock.getLowPrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(stock.getPrevClose(), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private void saveRankSnapshot(List<Stock> stocks) {
        String[] types = {"change", "fall", "volume", "amount", "turnover", "amplitude"};
        LocalDateTime now = LocalDateTime.now();
        for (String type : types) {
            List<StockRank> ranks = getRankList(type, Integer.MAX_VALUE);
            for (StockRank sr : ranks) {
                sr.setSnapshotTime(now);
                stockRankMapper.insert(sr);
            }
        }
    }

    private Map<String, Stock> loadStockMap() {
        List<Stock> stocks = stockMapper.selectList(null);
        return stocks.stream().collect(Collectors.toMap(Stock::getStockCode, s -> s));
    }
    @Override
    public void refreshRank(String rankType) {
        List<Stock> stocks = stockMapper.selectList(null);
        if (stocks.isEmpty()) return;

        String key = RANK_PREFIX + rankType;
        zSetOps.removeRange(key, 0, Integer.MAX_VALUE);

        for (Stock stock : stocks) {
            double score = switch (rankType) {
                case "change" -> computeChange(stock);
                case "fall" -> computeFall(stock);
                case "volume" -> stock.getVolume() != null ? stock.getVolume().doubleValue() : 0.0;
                case "amount" -> stock.getAmount() != null ? stock.getAmount().doubleValue() : 0.0;
                case "turnover" -> computeTurnover(stock);
                case "amplitude" -> computeAmplitude(stock);
                default -> 0.0;
            };
            zSetOps.add(key, stock.getStockCode(), score);
        }
        eventPublisher.publishEvent(new RankUpdatedEvent(rankType, null));
    }
}