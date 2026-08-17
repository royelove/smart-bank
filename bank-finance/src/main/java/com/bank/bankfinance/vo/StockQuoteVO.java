package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class StockQuoteVO {
    private String stockCode;
    private String stockName;
    private BigDecimal currentPrice;
    private BigDecimal prevClose;
    private BigDecimal changePercent;
    private LocalDateTime quoteTime;
}