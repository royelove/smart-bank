package com.bank.bankfinance.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportLoginVO {
    private String token;
    private String staffName;
    public SupportLoginVO() {}
    public SupportLoginVO(String token, String staffName) {
        this.token = token;
        this.staffName = staffName;
    }
}
