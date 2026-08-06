package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinancialRecommendVO {
    private FinancialProductVO product;
    private Integer matchScore;
    private String reason;
    private String userRiskLevel;
}
