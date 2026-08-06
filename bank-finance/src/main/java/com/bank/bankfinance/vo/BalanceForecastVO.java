package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class BalanceForecastVO {
    private String accountNumber;
    private BigDecimal currentBalance;
    private List<BalancePointVO> points;
    private BigDecimal minForecastBalance;
    private boolean lowRiskWarning;
}
