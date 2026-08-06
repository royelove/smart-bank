package com.bank.bankfinance.service.impl;

import com.bank.bankfinance.entity.FinancialHolding;
import com.bank.bankfinance.entity.FinancialProduct;
import com.bank.bankfinance.entity.Trade;
import com.bank.bankfinance.mapper.FinancialHoldingMapper;
import com.bank.bankfinance.mapper.FinancialProductMapper;
import com.bank.bankfinance.mapper.TradeMapper;
import com.bank.bankfinance.service.RecommendationService;
import com.bank.bankfinance.vo.FinancialProductVO;
import com.bank.bankfinance.vo.FinancialRecommendVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private FinancialProductMapper productMapper;

    @Autowired
    private FinancialHoldingMapper holdingMapper;

    @Autowired
    private TradeMapper tradeMapper;

    @Override
    public List<FinancialRecommendVO> recommend(String accountNumber, int topN) {
        double prefRate = preferenceRate(accountNumber);
        String riskLevel = riskLabel(prefRate);

        List<FinancialProduct> products = productMapper.selectList(new LambdaQueryWrapper<FinancialProduct>()
                .eq(FinancialProduct::getStatus, 1)
                .gt(FinancialProduct::getRemainingAmount, 0));

        List<FinancialRecommendVO> result = new ArrayList<>();
        for (FinancialProduct p : products) {
            double rate = p.getAnnualReturnRate() == null ? 0 : p.getAnnualReturnRate().doubleValue();
            double rateScore = Math.max(0, 100 - Math.abs(prefRate - rate) * 40);
            double termScore = termScore(p.getTermDays() == null ? 0 : p.getTermDays());
            double availScore = (p.getRemainingAmount() != null && p.getMinAmount() != null
                    && p.getRemainingAmount().compareTo(p.getMinAmount().multiply(new BigDecimal("10"))) >= 0) ? 10 : 5;

            int score = (int) Math.round(rateScore * 0.6 + termScore * 0.3 + availScore);

            FinancialRecommendVO vo = new FinancialRecommendVO();
            vo.setProduct(toVO(p));
            vo.setMatchScore(score);
            vo.setUserRiskLevel(riskLevel);
            vo.setReason(buildReason(rate, prefRate, score, p.getTermDays()));
            result.add(vo);
        }

        result.sort(Comparator.comparingInt(FinancialRecommendVO::getMatchScore).reversed());
        return result.stream().limit(Math.max(0, topN)).collect(Collectors.toList());
    }

    private double preferenceRate(String accountNumber) {
        List<FinancialHolding> holdings = holdingMapper.selectList(new LambdaQueryWrapper<FinancialHolding>()
                .eq(FinancialHolding::getAccountNumber, accountNumber)
                .eq(FinancialHolding::getStatus, 1));
        if (!holdings.isEmpty()) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal weightedRate = BigDecimal.ZERO;
            for (FinancialHolding h : holdings) {
                if (h.getInvestAmount() == null || h.getAnnualReturnRate() == null) continue;
                totalAmount = totalAmount.add(h.getInvestAmount());
                weightedRate = weightedRate.add(h.getAnnualReturnRate()
                        .multiply(h.getInvestAmount()));
            }
            if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
                return weightedRate.divide(totalAmount, 2, RoundingMode.HALF_UP).doubleValue();
            }
        }
        return preferenceRateFromTrades(accountNumber);
    }

    private double preferenceRateFromTrades(String accountNumber) {
        List<Trade> trades = tradeMapper.selectList(new LambdaQueryWrapper<Trade>()
                .and(w -> w.eq(Trade::getFromAccount, accountNumber)
                        .or()
                        .eq(Trade::getToAccount, accountNumber))
                .orderByDesc(Trade::getTradeDate)
                .last("LIMIT 50"));
        List<BigDecimal> amounts = trades.stream()
                .map(Trade::getAmount)
                .filter(a -> a != null)
                .collect(Collectors.toList());
        if (amounts.isEmpty()) {
            return 3.5;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal a : amounts) sum = sum.add(a);
        double avg = sum.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP).doubleValue();
        if (avg < 500) return 2.5;
        if (avg < 2000) return 3.5;
        if (avg < 10000) return 4.5;
        return 5.5;
    }

    private double termScore(int termDays) {
        if (termDays >= 90 && termDays <= 365) return 100;
        if (termDays >= 30) return 80;
        if (termDays > 0) return 60;
        return 70;
    }

    private String riskLabel(double rate) {
        if (rate < 3) return "保守型";
        if (rate < 4) return "稳健型";
        if (rate < 5) return "进取型";
        return "激进型";
    }

    private String buildReason(double rate, double prefRate, int score, Integer termDays) {
        StringBuilder sb = new StringBuilder();
        if (score >= 85) {
            sb.append("收益率与你的").append(riskLabel(prefRate)).append("偏好高度匹配");
        } else if (rate > prefRate + 0.5) {
            sb.append("收益高于你的习惯水平，可适当配置");
        } else if (rate < prefRate - 0.5) {
            sb.append("收益稳健，适合分散风险");
        } else {
            sb.append("与你的投资习惯相近");
        }
        if (termDays != null) {
            sb.append("，期限").append(termDays).append("天");
        }
        return sb.toString();
    }

    private FinancialProductVO toVO(FinancialProduct p) {
        FinancialProductVO vo = new FinancialProductVO();
        vo.setId(p.getId());
        vo.setProductCode(p.getProductCode());
        vo.setProductName(p.getProductName());
        vo.setAnnualReturnRate(p.getAnnualReturnRate());
        vo.setTermDays(p.getTermDays());
        vo.setMinAmount(p.getMinAmount());
        vo.setTotalAmount(p.getTotalAmount());
        vo.setRemainingAmount(p.getRemainingAmount());
        vo.setStatus(p.getStatus());
        vo.setDescription(p.getDescription());
        vo.setCreationDate(p.getCreationDate());
        return vo;
    }
}
