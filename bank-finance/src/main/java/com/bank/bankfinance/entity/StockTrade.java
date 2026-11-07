package com.bank.bankfinance.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_stock_trade")
public class StockTrade {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tradeNo;
    private String stockCode;
    private String stockName;
    private String buyOrderNo;
    private String sellOrderNo;
    private String buyAccount;
    private String sellAccount;
    private BigDecimal tradePrice;
    private Integer tradeQuantity;
    private BigDecimal tradeAmount;
    private LocalDateTime tradeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
