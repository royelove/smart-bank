package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class FinancialHoldingVO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal annualReturnRate;
    private Integer termDays;
    private BigDecimal investAmount;
    private BigDecimal expectedReturn;
    private Integer status;
    private LocalDateTime purchaseDate;
    private LocalDateTime redeemDate;
}
