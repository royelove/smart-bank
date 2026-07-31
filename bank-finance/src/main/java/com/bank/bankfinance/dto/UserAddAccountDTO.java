package com.bank.bankfinance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddAccountDTO {
    @NotBlank
    private String userIdCard;
    @NotBlank
    private String accountNumber;
}
