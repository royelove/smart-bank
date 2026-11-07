package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_stock_kline")
public class StockKline {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private String stockName;
    private String period;
    private BigDecimal openPrice;
    private BigDecimal closePrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    /** 成交量（手） */
    private Long volume;
    /** 成交额（元） */
    private BigDecimal amount;
    /** 本根柱子所属周期的起始时间 */
    private LocalDateTime klineTime;
    /** 数据来源：trade=真实撮合成交 / quote=实时行情合成 / simulated=历史不足时补齐的演示数据 */
    private String dataSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}