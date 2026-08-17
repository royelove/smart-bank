package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class IdCardLibVO {
    private Long id;
    private String idCard;
    private String name;
    private LocalDateTime creationDate;
}