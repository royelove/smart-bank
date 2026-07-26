package com.bank.bankfinance.entity;


import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@TableName("t_trade")
public class Trade {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String tradeNo;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String tradeType;
    private LocalDateTime tradeDate;
    private LocalDateTime updateDate;
    public Trade(){}
 }
