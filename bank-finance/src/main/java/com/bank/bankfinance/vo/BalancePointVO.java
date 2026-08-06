package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BalancePointVO {
    private String date;
    private BigDecimal predictedBalance;
}
