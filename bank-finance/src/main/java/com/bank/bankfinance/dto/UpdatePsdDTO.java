package com.bank.bankfinance.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Pattern;

@Getter
@Setter
public class UpdatePsdDTO {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;
    @NotBlank(message = "新密码必须是6位数字")
    @Pattern(regexp = "^\\d{6}$", message = "密码必须是6位纯数字")
    private String newPassword;
    @NotBlank(message = "确认密码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "确认密码必须是6位纯数字")
    private String confirmPassword;
}
