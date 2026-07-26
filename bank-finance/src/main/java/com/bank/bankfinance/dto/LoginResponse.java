package com.bank.bankfinance.dto;

import com.bank.bankfinance.vo.AccountVO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {
    private String token;
    private AccountVO accountVO;
    LoginResponse(){}
    public LoginResponse(String token, AccountVO accountVO) {
        this.token = token;
        this.accountVO = accountVO;
    }
}
