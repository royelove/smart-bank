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
    // 修复：原与 TRANSACTION_INVALID_FREEZE 共用 "500001"，客户端无法区分是哪个账户被冻结
    TRANSACTION_INVALID_FREEZE_TRANSFER("500004","转入账户已被冻结，无法进行任何操作"),
    TRANSACTION_INVALID_INSUFFICIENT("500002","账户余额不足"),
    TRANSACTION_RISK_REJECTED("500003","交易被风控系统拦截"),
    TRANSACTION_INVALID_NOT_SAME_CODE("600001","两次密码不同"),
    TRANSACTION_INVALID_SAME_ACCOUNTNUMBER("600002","账户已存在"),
    TRANSACTION_INVALID_SELF_TRANSFER("600003","不能给自己转账"),
    ACCOUNT_EXISTS("600004","卡号已存在"),
    PASSWORD_TOO_SHORT("600005","密码长度不能小于6位"),
    USER_NOT_FOUND("600006","用户不存在"),
    USER_EXISTS("600007","该账号已存在"),
    USER_PASSWORD_INVALID("600008","密码错误"),
    ID_CARD_MISMATCH("600009","身份证号不匹配"),
    ACCOUNT_NOT_FOUND("600010","账户不存在"),
    ACCOUNT_NOT_OWNED("600011","该卡不属于此身份证号"),
    OLD_PASSWORD_INVALID("600012","原密码错误"),
    SUPPORT_LOGIN_ERROR("600013","账号或密码错误"),
    TICKET_TYPE_INVALID("600014","无效的问题类型"),
    TICKET_ALREADY_ACCEPTED("600015","该工单已被受理"),
    TICKET_CLOSED_REPLY("600016","工单已关闭，无法回复"),
    TICKET_CLOSED("600017","工单已关闭"),
    TICKET_NOT_FOUND("600018","工单不存在"),

    NO_ADMIN_PERMISSION("700001","无管理员权限"),
    NO_LOGIN("700002","用户未登录"),
    /*
     * 新增：token 缺失/非法/已登出的统一语义。
     * 原来这个 "401000" 只以字符串字面量的形式出现在 JwtAuthenticationFilter 与
     * GlobalExceptionHandler 里，没有对应的枚举常量，属于「隐式协议」；现在收敛为常量，
     * 两处引用同一来源（JwtAuthenticationFilter.reject / GlobalExceptionHandler）。
     */
    TOKEN_INVALID("401000","登录已失效，请重新登录"),
    /** 新增：限流命中。此前 RateLimitAspect 抛的是临时拼的字符串码。 */
    RATE_LIMITED("429000","请求过于频繁，请稍后再试"),
    // 新增：客服自助注册的可选密钥闸门（app.support.staff-register-key 非空时生效）
    STAFF_REGISTER_FORBIDDEN("700003","客服注册未通过校验，请联系管理员"),
    UPDATE_FAILED("800001","更新失败"),
    VERIFICATION_CODE_INVALID("900001","验证码错误或已过期"),
    PHONE_NUMBER_MISMATCH("900002","手机号与账户不匹配"),
    FACE_NO_FACE("910001","未检测到人脸，请正对镜头重试"),
    FACE_NOT_BOUND("910002","请先绑定人脸"),
    FACE_MISMATCH("910003","人脸验证未通过，请重试"),
    FACE_SERVICE_ERROR("910004","人脸识别服务不可用，请稍后再试"),
    ID_CARD_NOT_FOUND("910005","身份库中未找到该身份证号，请联系客服录入"),
    ID_CARD_REQUIRED("910006","请先在个人信息中完善身份证号"),
    ID_CARD_EXISTS("910007","该身份证号已录入"),
    STOCK_CODE_INVALID("920001","股票不存在或已停牌"),
    STOCK_QTY_INVALID("920002","委托数量须为100股的整数倍"),
    STOCK_PRICE_INVALID("920003","委托价格必须大于0"),
    STOCK_FUND_INSUFFICIENT("920004","可用资金不足"),
    STOCK_SHARE_INSUFFICIENT("920005","可用持仓不足"),
    STOCK_ORDER_NOT_CANCELLABLE("920006","仅待成交委托可撤单"),
    STOCK_QUOTE_UNAVAILABLE("920007","当前无行情，请稍后再试"),
    STOCK_DIRECTION_INVALID("920008","无效的交易方向"),
    STOCK_PRICE_TYPE_INVALID("920009","无效的委托类型"),
    STOCK_KLINE_PERIOD_INVALID("920010","不支持的K线周期"),
    IDEMPOTENT_REJECTED("600019", "请勿重复提交"),
    // 新增：并发 check-then-insert 撞唯一索引时使用，替代原先误导性的「系统繁忙」
    DUPLICATE_RECORD("600020", "记录已存在，请勿重复提交");
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
