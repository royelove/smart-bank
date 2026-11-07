package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_order_book")
public class OrderBookEntry {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String accountNumber;
    private String orderNo;
    private String stockCode;
    private String stockName;
    private String direction;
    private BigDecimal price;
    private BigDecimal quantity;
    private Integer filledQuantity;
    private Integer remainingQuantity;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Version
    private Integer version;

    public OrderBookEntry() {}

    public boolean isBuy() { return "BUY".equalsIgnoreCase(direction); }
    public boolean isSell() { return "SELL".equalsIgnoreCase(direction); }
    public boolean isActive() { return status != null && (status == 0 || status == 1); }
}