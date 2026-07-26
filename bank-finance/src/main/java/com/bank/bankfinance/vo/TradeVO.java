package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter
@Setter
public class TradeVO {
    private String tradeNo;
    private String fromAccountNo;
    private String toAccountNo;
    private String fromName;
    private String toName;
    private BigDecimal amount;
    private Integer tradeType;
    private LocalDateTime tradeDate;
    TradeVO(){}
}
