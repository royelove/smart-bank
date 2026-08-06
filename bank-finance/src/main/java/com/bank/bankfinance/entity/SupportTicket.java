package com.bank.bankfinance.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("t_support_ticket")
public class SupportTicket {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("ticket_no")
    private String ticketNo;
    @TableField("account_number")
    private String accountNumber;
    @TableField("user_name")
    private String userName;
    @TableField("user_phone")
    private String userPhone;
    @TableField("category")
    private String category;
    @TableField("title")
    private String title;
    @TableField("description")
    private String description;
    @TableField("status")
    private Integer status;
    @TableField("reply")
    private String reply;
    @TableField("handler_id")
    private Long handlerId;
    @TableField("handler_name")
    private String handlerName;
    @TableField("sentiment")
    private String sentiment;
    @TableField("ai_score")
    private Integer aiScore;
    @TableField("ai_valid")
    private Integer aiValid;
    @TableField("ai_eval")
    private String aiEval;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime creationDate;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateDate;
    @TableField("close_date")
    private LocalDateTime closeDate;
    public SupportTicket() {}
}
