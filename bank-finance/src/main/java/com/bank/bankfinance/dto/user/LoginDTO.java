package com.bank.bankfinance.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {
    @NotBlank(message = "卡号不能为空")
    private String accountNumber;
    @NotBlank(message = "密码不能为空")
    private String password;
}
