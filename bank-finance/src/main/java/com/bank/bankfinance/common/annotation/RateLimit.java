package com.bank.bankfinance.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解。
 *
 * <p>维度说明：默认同时按「客户端 IP」与「业务主体（卡号 / 用户 ID / 客服 ID）」两个维度计数，
 * 两者任一超限即拒绝。只按 IP 限流有两个漏洞：NAT 后的正常用户会互相挤掉；攻击者轮换 IP 即可绕过。
 *
 * @see com.bank.bankfinance.common.aspect.RateLimitAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 时间窗口内允许的最大请求数（IP 维度与主体维度各自独立计数） */
    int maxRequests() default 10;

    int timeWindow() default 1;

    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 是否启用「业务主体」维度。默认开启。
     *
     * <p>主体取值顺序：已登录身份（{@code LoginContext}）→ 请求参数中的
     * {@code accountNumber} / {@code accountNo} / {@code inNumber} 字段。
     * 若两者都取不到，则退化为仅按 IP 限流。
     */
    boolean perAccount() default true;

    String message() default "请求过于频繁，请稍后再试";
}
