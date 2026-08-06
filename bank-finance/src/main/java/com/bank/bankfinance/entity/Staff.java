package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_staff")
public class Staff {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("username")
    private String username;
    @TableField("password")
    private String password;
    @TableField("staff_name")
    private String staffName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;
    public Staff() {}
}
