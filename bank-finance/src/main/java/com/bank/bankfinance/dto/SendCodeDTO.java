package com.bank.bankfinance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendCodeDTO {
    @NotBlank(message = "账号不能为空")
    private String accountNumber;
    @NotBlank(message = "手机号不能为空")
    private String userPhoneNumber;
}
