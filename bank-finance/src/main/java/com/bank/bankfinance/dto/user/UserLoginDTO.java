package com.bank.bankfinance.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginDTO {
    @NotBlank
    private String userPhone;
    @NotBlank
    private String password;
}
