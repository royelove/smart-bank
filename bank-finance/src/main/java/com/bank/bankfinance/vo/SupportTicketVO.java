package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SupportTicketVO {
    private Long id;
    private String ticketNo;
    private String accountNumber;
    private String userName;
    private String userPhone;
    private String category;
    private String categoryText;
    private String title;
    private String description;
    private Integer status;
    private String statusText;
    private String reply;
    private String handlerName;
    private String sentiment;
    private Integer aiScore;
    private Integer aiValid;
    private String aiEval;
    private LocalDateTime creationDate;
    private LocalDateTime closeDate;
}
