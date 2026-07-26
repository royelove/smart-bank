package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//直接注入set，get方法，tableName注解与相关数据库表关联
@Getter
@Setter
@TableName("t_account")
public class Account {
    //主键自增
    @TableId(type = IdType.AUTO)
    private Long id;

    private String accountNumber;
    private String password;
    private String accountHolderName;
    private String accountType;
    private BigDecimal accountBalance;

    //创建时，框架自动填充时间，无法修改
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    //可以修改，为更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime  updateDate;
    //其中0代表管理员，1代表普通用户
    private String role;
    private Integer accountStatus;
    private String userPhoneNumber;
    public Account(){}

}
