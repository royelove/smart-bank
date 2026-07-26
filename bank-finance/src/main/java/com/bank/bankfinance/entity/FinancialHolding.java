package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_financial_holding")
public class FinancialHolding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String accountNumber;
    private Long productId;
    private String productName;
    private BigDecimal annualReturnRate;
    private Integer termDays;
    private BigDecimal investAmount;
    private Integer status;
    private LocalDateTime purchaseDate;
    private LocalDateTime redeemDate;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;
    public FinancialHolding() {}
}
