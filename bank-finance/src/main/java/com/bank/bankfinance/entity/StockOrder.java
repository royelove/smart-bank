package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_order")
public class StockOrder {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;
    private String accountNumber;
    private String stockCode;
    private String stockName;
    /** BUY / SELL */
    private String direction;
    /** MARKET / LIMIT */
    private String priceType;
    private BigDecimal orderPrice;
    private Integer orderQuantity;
    /** 买入时冻结资金（一键成交场景下为0） */
    private BigDecimal frozenAmount;
    private BigDecimal filledAmount;
    private Integer filledQuantity;
    /** 0=待成交 1=已成交 2=已撤单 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    public StockOrder() {}
}