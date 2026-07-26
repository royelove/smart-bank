package com.bank.bankfinance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDTO {
    @NotBlank(message = "账号不能为空")
    private String accountNumber;
    @NotBlank(message = "验证码不能为空")
    private String code;
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "密码必须是6位纯数字")
    private String newPassword;
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
