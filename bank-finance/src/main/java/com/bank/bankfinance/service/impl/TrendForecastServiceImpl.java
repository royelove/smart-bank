package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.entity.Account;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.TradeMapper;
import com.bank.bankfinance.service.TrendForecastService;
import com.bank.bankfinance.vo.BalanceForecastVO;
import com.bank.bankfinance.vo.BalancePointVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TrendForecastServiceImpl implements TrendForecastService {

    private static final int HISTORY_DAYS = 30;
    private static final int FORECAST_DAYS = 7;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private TradeMapper tradeMapper;

    @Override
    public BalanceForecastVO forecast(String accountNumber) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getAccountNumber, accountNumber));
        BalanceForecastVO vo = new BalanceForecastVO();
        vo.setAccountNumber(accountNumber);
        if (account == null) {
            vo.setCurrentBalance(BigDecimal.ZERO);
            vo.setPoints(new ArrayList<>());
            vo.setMinForecastBalance(BigDecimal.ZERO);
            vo.setLowRiskWarning(false);
            return vo;
        }
        BigDecimal currentBalance = account.getAccountBalance();
        vo.setCurrentBalance(currentBalance);

        Map<LocalDate, BigDecimal> dailyNet = dailyNetFlow(accountNumber);
        List<Double> netSeries = buildSeries(dailyNet);

        double[] slope = linearRegression(netSeries);
        List<BalancePointVO> points = new ArrayList<>();
        BigDecimal balance = currentBalance;
        BigDecimal min = currentBalance;
        LocalDate start = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        for (int k = 0; k < FORECAST_DAYS; k++) {
            double predictedNet = predict(slope, netSeries.size() + k);
            balance = balance.add(BigDecimal.valueOf(predictedNet).setScale(2, RoundingMode.HALF_UP));
            if (balance.compareTo(min) < 0) {
                min = balance;
            }
            BalancePointVO point = new BalancePointVO();
            point.setDate(start.plusDays(k + 1).format(fmt));
            point.setPredictedBalance(balance);
            points.add(point);
        }

        vo.setPoints(points);
        vo.setMinForecastBalance(min);
        vo.setLowRiskWarning(min.compareTo(BigDecimal.ZERO) < 0);
        return vo;
    }

    private Map<LocalDate, BigDecimal> dailyNetFlow(String accountNumber) {
        List<Trade> trades = tradeMapper.selectList(new LambdaQueryWrapper<Trade>()
                .and(w -> w.eq(Trade::getFromAccount, accountNumber)
                        .or()
                        .eq(Trade::getToAccount, accountNumber))
                .ge(Trade::getTradeDate, LocalDate.now().minusDays(HISTORY_DAYS).atStartOfDay())
                .orderByAsc(Trade::getTradeDate));

        Map<LocalDate, BigDecimal> daily = new LinkedHashMap<>();
        for (Trade t : trades) {
            if (t.getTradeDate() == null || t.getAmount() == null) continue;
            LocalDate day = t.getTradeDate().toLocalDate();
            BigDecimal delta = accountNumber.equals(t.getToAccount()) ? t.getAmount() : t.getAmount().negate();
            daily.merge(day, delta, BigDecimal::add);
        }
        return daily;
    }

    private List<Double> buildSeries(Map<LocalDate, BigDecimal> daily) {
        List<Double> series = daily.values().stream()
                .map(BigDecimal::doubleValue)
                .collect(Collectors.toList());
        if (series.size() < 3) {
            series.clear();
        }
        return series;
    }

    private double[] linearRegression(List<Double> y) {
        if (y.isEmpty()) {
            return new double[]{0, 0};
        }
        int m = y.size();
        double sumX = 0, sumY = 0;
        for (int i = 0; i < m; i++) {
            sumX += i;
            sumY += y.get(i);
        }
        double meanX = sumX / m;
        double meanY = sumY / m;
        double num = 0, den = 0;
        for (int i = 0; i < m; i++) {
            num += (i - meanX) * (y.get(i) - meanY);
            den += (i - meanX) * (i - meanX);
        }
        double b = (den == 0) ? 0 : num / den;
        double a = meanY - b * meanX;
        return new double[]{a, b};
    }

    private double predict(double[] ab, int x) {
        return ab[0] + ab[1] * x;
    }
}
