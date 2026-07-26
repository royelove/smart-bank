package com.bank.bankfinance.common.exception;

import com.bank.bankfinance.common.constant.ResponseCode;
import lombok.Getter;
@Getter
public class BusinessException extends RuntimeException {
    private ResponseCode responseCode;
    private String code;

    public BusinessException(ResponseCode responseCode) {
        super(responseCode.getMsg());
        this.responseCode = responseCode;
        this.code = responseCode.getCode();
    }

    public BusinessException(String code, String msg) {
        super(msg);
        this.code = code;
    }
}
