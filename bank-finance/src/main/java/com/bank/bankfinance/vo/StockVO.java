package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class StockVO {
    private Long id;
    private String stockCode;
    private String stockName;
    private String market;
    private Integer status;
    private BigDecimal prevClose;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
}