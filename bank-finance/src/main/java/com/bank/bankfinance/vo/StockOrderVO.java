package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class StockOrderVO {
    private Long id;
    private String orderNo;
    private String accountNumber;
    private String stockCode;
    private String stockName;
    private String direction;
    private String priceType;
    private BigDecimal orderPrice;
    private Integer orderQuantity;
    private BigDecimal filledAmount;
    private Integer filledQuantity;
    private Integer status;
    private LocalDateTime creationDate;
}