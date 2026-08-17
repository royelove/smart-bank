package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_position")
public class Position {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountNumber;
    private String stockCode;
    private String stockName;
    private Integer quantity;
    private BigDecimal avgCost;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    public Position() {}
}