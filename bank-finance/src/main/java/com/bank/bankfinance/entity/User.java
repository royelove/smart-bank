package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_phone")
    private String userPhone;

    @TableField("password")
    private String password;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    @TableField("id_card_no")
    private String userIdCard;
}
