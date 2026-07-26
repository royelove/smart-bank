package com.bank.bankfinance.common.aspect;

import com.bank.bankfinance.common.annotation.RateLimit;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class RateLimitAspect {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    @Autowired
    private RedisUtil redisUtil;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        String ip = getClientIp(request);
        String methodName = joinPoint.getSignature().toShortString();
        String key = RATE_LIMIT_PREFIX + ip + ":" + methodName;

        Long count = redisUtil.increment(key);
        if (count == 1) {
            redisUtil.expire(key, rateLimit.timeWindow(), rateLimit.timeUnit());
        }
        if (count > rateLimit.maxRequests()) {
            throw new BusinessException("429000", rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
