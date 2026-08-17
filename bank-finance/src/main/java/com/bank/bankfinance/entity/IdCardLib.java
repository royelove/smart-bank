package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_id_card_lib")
public class IdCardLib {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String idCard;
    private String name;

    /** 人脸特征(512维)，逗号分隔字符串 */
    private String faceFeature;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;

    public IdCardLib() {}
}