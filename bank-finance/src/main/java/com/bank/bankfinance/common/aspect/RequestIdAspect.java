package com.bank.bankfinance.common.aspect;

import com.bank.bankfinance.common.util.TicketNoUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Aspect
@Component
public class RequestIdAspect {
    //所有controller下面的所有方法在执行之前都需要经过这个切面
    @Around("execution(* com.bank.bankfinance.controller.*.*(..))")
    public Object generateRequestId(ProceedingJoinPoint pjp) throws Throwable {
        //获取目标方法的所有参数
        Object[] args = pjp.getArgs();
        for (Object arg : args) {
            if (arg != null) {
                try {
                    //用反射去找对象里字段为requestId的字段
                    Field field = arg.getClass().getDeclaredField("requestId");
                    field.setAccessible(true);
                    String requestId = (String) field.get(arg);
                    if (requestId == null || requestId.isEmpty()) {
                        field.set(arg, TicketNoUtil.requestId());
                    }
                } catch (NoSuchFieldException e) {
                    // 这个 DTO 没有 requestId 字段，跳过
                }
            }
        }
        return pjp.proceed();
    }
}