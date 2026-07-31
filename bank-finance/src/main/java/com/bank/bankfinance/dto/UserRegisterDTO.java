package com.bank.bankfinance.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterDTO {
    @NotEmpty
    private String userPhone;
    @NotBlank
    private String password;
    @NotBlank
    private String checkPassword;

    @NotBlank
    private String userIdCard;
    UserRegisterDTO() {}
}
