package com.bank.bankfinance.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

@Getter
@Setter
public class RegisterDTO {
    @NotEmpty
    private String accountNumber;
    @NotEmpty
    @Valid
    @Pattern(regexp = "^\\d{6}$", message = "密码必须是6位纯数字")
    private String password;
    @NotEmpty
    private String checkPassword;

    @NotEmpty
    private String accountHolderName;
    @NotEmpty
    private String accountType;
    private BigDecimal accountBalance;
    @NotEmpty
    private String userPhoneNumber;

    @NotEmpty
    private String userIdCard;
    RegisterDTO(){}
}
