package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_financial_product")
public class FinancialProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    private BigDecimal annualReturnRate;
    private Integer termDays;
    private BigDecimal minAmount;
    private BigDecimal totalAmount;
    private BigDecimal remainingAmount;
    private Integer status;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;
    public FinancialProduct() {}
}
