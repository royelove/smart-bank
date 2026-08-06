package com.bank.bankfinance.common.constant;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum ResponseCode {
    SUCCESS("000000","操作成功"),
    FAIL("999999","系统繁忙，请稍后重试"),


    BANK_ID_INVALID("400001","银行卡账号错误"),
    PASSWORD_INVALID("400002","银行卡密码错误"),

    TRANSACTION_INVALID_FREEZE("500001","该账户已被冻结，无法进行任何操作"),
    TRANSACTION_INVALID_FREEZE_TRANSFER("500001","转入账户已被冻结，无法进行任何操作"),
    TRANSACTION_INVALID_INSUFFICIENT("500002","账户余额不足"),
    TRANSACTION_RISK_REJECTED("500003","交易被风控系统拦截"),
    TRANSACTION_INVALID_NOT_SAME_CODE("600001","两次密码不同"),
    TRANSACTION_INVALID_SAME_ACCOUNTNUMBER("600002","账户已存在"),

    NO_ADMIN_PERMISSION("700001","无管理员权限"),
    NO_LOGIN("700002","用户未登录"),
    UPDATE_FAILED("800001","更新失败"),
    VERIFICATION_CODE_INVALID("900001","验证码错误或已过期"),
    PHONE_NUMBER_MISMATCH("900002","手机号与账户不匹配"),
    FACE_NO_FACE("910001","未检测到人脸，请正对镜头重试"),
    FACE_NOT_BOUND("910002","请先绑定人脸"),
    FACE_MISMATCH("910003","人脸验证未通过，请重试"),
    FACE_SERVICE_ERROR("910004","人脸识别服务不可用，请稍后再试");
    public static final String ACCOUNT_NOT_FOUND = "用户不存在";
    private String code;
    private String msg;
    ResponseCode(String code,String msg){
        this.code = code;
        this.msg = msg;
    }
    public String getCode(){
        return this.code;
    }
    public String getMsg(){
        return this.msg;
    }

}
