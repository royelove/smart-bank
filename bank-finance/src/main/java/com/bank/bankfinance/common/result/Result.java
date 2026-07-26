package com.bank.bankfinance.common.result;
import com.bank.bankfinance.common.constant.ResponseCode;
import lombok.Data;
@Data
public class Result<T> {
    private String code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data){
        Result<T> result = new Result<T>();
        result.setCode(ResponseCode.SUCCESS.getCode());
        result.setMsg(ResponseCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }
    public static <T> Result<T> fail(ResponseCode responseCode){
        Result<T> result = new Result<T>();
        result.setCode(responseCode.getCode());
        result.setMsg(responseCode.getMsg());
        return result;
    }
    public static <T> Result<T> fail(String code,String msg){
        Result<T> result = new Result<T>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
