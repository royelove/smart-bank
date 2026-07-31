package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AccountVO {
    private String accountNumber;
    private String accountHolderName;
    private String accountType;
    private BigDecimal accountBalance;
    private Integer accountStatus;
    private String userPhoneNumber;
    private String userIdCard;
    private Long userId;
    public AccountVO(){}
}
