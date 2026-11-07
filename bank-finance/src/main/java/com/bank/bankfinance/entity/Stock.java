package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_stock")
public class Stock {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private String stockName;
    private String market;
    private Integer status;
    private BigDecimal prevClose;
    private BigDecimal currentPrice;
    private BigDecimal changePercent;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal turnoverRate;
    private BigDecimal totalShares;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    public Stock() {}
}