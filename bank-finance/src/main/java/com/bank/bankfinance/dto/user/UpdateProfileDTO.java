package com.bank.bankfinance.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileDTO {
    @NotBlank(message = "户名不能为空")
    private String accountHolderName;
    @NotBlank(message = "手机号不能为空")
    private String userPhoneNumber;
}
