package com.bank.bankfinance.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxRequests() default 10;
    int timeWindow() default 1;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    String message() default "请求过于频繁，请稍后再试";
}
