package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class FinancialProductVO {
    private Long id;
    private String productCode;
    private String productName;
    private BigDecimal annualReturnRate;
    private Integer termDays;
    private BigDecimal minAmount;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private Integer status;
    private String description;
    private LocalDateTime creationDate;
}
