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

    @TableField("user_name")
    private String userName;

    @TableField("password")
    private String password;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    @TableField("id_card_no")
    private String userIdCard;

    @TableField("face_feature")
    private String faceFeature;

    /** 0=未绑定 1=已绑定 */
    @TableField("face_bound")
    private Integer faceBound;

    /** 0=未实名 1=已实名 */
    @TableField("realname_bound")
    private Integer realnameBound;
}
