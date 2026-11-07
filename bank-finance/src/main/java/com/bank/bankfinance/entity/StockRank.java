package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_stock_rank")
public class StockRank {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;
    private String stockName;
    private String market;
    private String rankType;
    private BigDecimal rankValue;
    private BigDecimal prevClose;
    private BigDecimal currentPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private Long volume;
    private BigDecimal amount;
    private BigDecimal turnoverRate;
    @TableField("`rank`")
    private Integer rank;
    private LocalDateTime snapshotTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}