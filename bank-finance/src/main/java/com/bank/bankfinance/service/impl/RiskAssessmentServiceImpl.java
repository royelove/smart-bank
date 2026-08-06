package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.common.algo.IsolationForest;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.RedisUtil;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.mapper.TradeMapper;
import com.bank.bankfinance.service.RiskAssessmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.bank.bankfinance.common.constant.ResponseCode.TRANSACTION_RISK_REJECTED;

@Service
public class RiskAssessmentServiceImpl implements RiskAssessmentService {

    private static final String RISK_FREQ_PREFIX = "risk:freq:";
    private static final int FREQ_WINDOW_MINUTES = 5;
    private static final int FREQ_MAX = 5;

    private static final int SCORE_LARGE_AMOUNT = 30;
    private static final int SCORE_DEVIATE_AVG = 30;
    private static final int SCORE_LATE_NIGHT = 20;
    private static final int SCORE_HIGH_FREQ = 25;
    private static final int SCORE_NEW_COUNTERPARTY = 15;
    private static final int SCORE_ISOLATION_FOREST = 30;
    private static final int BLOCK_THRESHOLD = 60;

    private static final double ANOMALY_THRESHOLD = 0.62;
    private static final long FOREST_TTL_MILLIS = 10 * 60 * 1000L;
    private final Map<String, CachedForest> forestCache = new ConcurrentHashMap<>();

    @Autowired
    private TradeMapper tradeMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void assessAndBlock(String accountNumber, BigDecimal amount, String counterparty) {
        List<String> hitRules = new ArrayList<>();
        int score = 0;

        // 规则1：单笔超大额
        if (amount.compareTo(new BigDecimal("10000")) >= 0) {
            score += SCORE_LARGE_AMOUNT;
            hitRules.add("单笔金额超限");
        }

        // 规则2：偏离历史平均金额（统计偏离，历史不足3笔跳过）
        List<BigDecimal> historyAmounts = recentAmounts(accountNumber, 20);
        if (historyAmounts.size() >= 3) {
            BigDecimal avg = average(historyAmounts);
            if (avg.compareTo(BigDecimal.ZERO) > 0
                    && amount.compareTo(avg.multiply(new BigDecimal("5"))) > 0) {
                score += SCORE_DEVIATE_AVG;
                hitRules.add("金额远超历史交易水平");
            }
        }

        // 规则3：深夜交易
        int hour = LocalDateTime.now().getHour();
        if (hour >= 23 || hour < 5) {
            score += SCORE_LATE_NIGHT;
            hitRules.add("深夜交易");
        }

        // 规则4：短时间频繁交易（Redis 滑动计数）
        String freqKey = RISK_FREQ_PREFIX + accountNumber;
        Long count = redisUtil.increment(freqKey);
        if (count == 1L) {
            redisUtil.expire(freqKey, FREQ_WINDOW_MINUTES, TimeUnit.MINUTES);
        }
        if (count >= FREQ_MAX) {
            score += SCORE_HIGH_FREQ;
            hitRules.add("短期内交易过于频繁");
        }

        // 规则5：首次向该对手方转账
        if (counterparty != null && !counterparty.isEmpty()) {
            Long exist = tradeMapper.selectCount(new LambdaQueryWrapper<Trade>()
                    .eq(Trade::getFromAccount, accountNumber)
                    .eq(Trade::getToAccount, counterparty));
            if (exist == 0L) {
                score += SCORE_NEW_COUNTERPARTY;
                hitRules.add("首次向该账户转账");
            }
        }

        // 规则6：孤立森林无监督异常检测（基于金额/时点/间隔三维特征）
        double ifScore = isolationForestScore(accountNumber, amount, LocalDateTime.now());
        if (ifScore >= 0) {
            if (ifScore > ANOMALY_THRESHOLD) {
                score += SCORE_ISOLATION_FOREST;
                hitRules.add("交易行为偏离历史规律");
            }
        }

        if (score >= BLOCK_THRESHOLD) {
            throw new BusinessException(TRANSACTION_RISK_REJECTED.getCode(),
                    "交易被风控系统拦截（" + String.join("、", hitRules) + "）");
        }
    }

    private double isolationForestScore(String accountNumber, BigDecimal amount, LocalDateTime now) {
        List<Trade> history = tradeMapper.selectList(new LambdaQueryWrapper<Trade>()
                .and(w -> w.eq(Trade::getFromAccount, accountNumber)
                        .or()
                        .eq(Trade::getToAccount, accountNumber))
                .orderByAsc(Trade::getTradeDate)
                .last("LIMIT 100"));
        if (history.size() < 20) {
            return -1;
        }

        double[][] samples = new double[history.size()][3];
        LocalDate prev = null;
        for (int i = 0; i < history.size(); i++) {
            Trade t = history.get(i);
            LocalDateTime dt = t.getTradeDate();
            samples[i][0] = t.getAmount() == null ? 0 : t.getAmount().doubleValue();
            samples[i][1] = dt == null ? 0 : dt.getHour();
            samples[i][2] = (dt != null && prev != null) ? ChronoUnit.DAYS.between(prev, dt.toLocalDate()) : 0;
            if (dt != null) {
                prev = dt.toLocalDate();
            }
        }

        CachedForest cached = forestCache.get(accountNumber);
        IsolationForest forest;
        if (cached == null || System.currentTimeMillis() - cached.timestamp > FOREST_TTL_MILLIS) {
            forest = new IsolationForest(samples, 60, 64);
            forestCache.put(accountNumber, new CachedForest(forest, System.currentTimeMillis()));
        } else {
            forest = cached.forest;
        }

        double[] feature = new double[]{amount.doubleValue(), now.getHour(), 0.0};
        return forest.score(feature);
    }

    private List<BigDecimal> recentAmounts(String accountNumber, int limit) {
        List<Trade> trades = tradeMapper.selectList(new LambdaQueryWrapper<Trade>()
                .and(w -> w.eq(Trade::getFromAccount, accountNumber)
                        .or()
                        .eq(Trade::getToAccount, accountNumber))
                .orderByDesc(Trade::getTradeDate)
                .last("LIMIT " + limit));
        return trades.stream()
                .map(Trade::getAmount)
                .filter(a -> a != null)
                .collect(Collectors.toList());
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            sum = sum.add(v);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private static class CachedForest {
        final IsolationForest forest;
        final long timestamp;

        CachedForest(IsolationForest forest, long timestamp) {
            this.forest = forest;
            this.timestamp = timestamp;
        }
    }
}
