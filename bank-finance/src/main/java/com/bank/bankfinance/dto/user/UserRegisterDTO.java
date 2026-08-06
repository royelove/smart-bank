package com.bank.bankfinance.dto.user;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterDTO {
    @NotEmpty
    private String userPhone;
    @NotBlank(message = "用户名不能为空")
    private String userName;
    @NotBlank
    private String password;
    @NotBlank
    private String checkPassword;

    @NotBlank
    private String userIdCard;
    UserRegisterDTO() {}
}
