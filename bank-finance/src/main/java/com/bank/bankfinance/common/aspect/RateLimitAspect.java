package com.bank.bankfinance.common.aspect;

import com.bank.bankfinance.common.annotation.RateLimit;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 限流切面。
 *
 * <p>修复记录（三个缺陷）：
 * <ol>
 *   <li><b>INCR 与 EXPIRE 非原子，会造成永久封禁。</b>原实现先 {@code increment}，
 *       再在 {@code count == 1} 时单独 {@code expire}。若两者之间发生异常、进程重启或
 *       Redis 抖动，该 key 将永远没有 TTL；此后 {@code count} 恒大于 1，再也进不了
 *       {@code expire} 分支，该 IP 从此永久无法登录且无自愈路径。现改为用 Lua 脚本
 *       原子完成，并且只要发现 TTL 缺失（{@code TTL < 0}）就补设，历史脏 key 也能自愈。</li>
 *   <li><b>客户端 IP 可伪造。</b>原实现无条件信任 {@code X-Forwarded-For} 的第一个值，
 *       攻击者每次请求带一个随机头就得到全新计数器，限流形同虚设。现仅在
 *       {@code app.security.trust-forwarded-headers=true}（即确实部署在可信反向代理之后）
 *       时才读取该头，否则一律使用 {@code getRemoteAddr()}。</li>
 *   <li><b>只有 IP 一个维度。</b>现增加业务主体维度（卡号/用户/客服），
 *       使攻击者无法通过轮换 IP 绕过密码 / 验证码暴力破解。</li>
 * </ol>
 *
 * <p>失败策略：Redis 不可用时<b>放行并告警</b>。限流是保护措施，不应把一次缓存抖动
 * 升级为全站不可用；安全性依赖的是随后的密码校验本身。
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 原子限流脚本。
     *
     * <p>{@code INCR} 后若为首次计数、或 TTL 已丢失，则补设过期时间。
     * 返回值即当前窗口内的请求数。
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>("""
            local n = redis.call('INCR', KEYS[1])
            if n == 1 or redis.call('TTL', KEYS[1]) < 0 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return n
            """, Long.class);

    /**
     * 业务主体字段候选名（覆盖本项目各 DTO 的命名习惯）。
     *
     * <p>顺序有意义：转账的 {@code outNumber} 是<b>付款方</b>（转出账户，也是密码校验与风控
     * 所针对的账户），{@code inNumber} 是收款方。必须优先取付款方，否则攻击者固定付款账户、
     * 轮换收款账户即可绕过主体维度限流。
     */
    private static final String[] SUBJECT_FIELDS = {
            "accountNumber", "accountNo", "outNumber", "inNumber",
            "userPhoneNumber", "userPhone", "phone", "username"
    };

    @Autowired
    private RedisUtil redisUtil;

    @Value("${app.security.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // @RateLimit 只标注在 Controller 上，正常一定有请求上下文；
        // 非常规调用（如内部任务直接调 Service）时没有可限流的对象，直接放行。
        if (attributes == null) {
            log.debug("[限流] 无请求上下文，跳过限流 method={}", joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        long windowSeconds = toSeconds(rateLimit);
        String method = resolveMethodName(joinPoint);
        String ip = getClientIp(request);

        // 维度一：客户端 IP
        if (exceeded(RATE_LIMIT_PREFIX + "ip:" + ip + ":" + method,
                rateLimit.maxRequests(), windowSeconds)) {
            log.warn("[限流] IP 维度触发 uri={} ip={} method={}", request.getRequestURI(), ip, method);
            throw new BusinessException("429000", rateLimit.message());
        }

        // 维度二：业务主体（卡号 / 用户 ID / 客服 ID）
        if (rateLimit.perAccount()) {
            String subject = resolveSubject(joinPoint);
            if (subject != null) {
                if (exceeded(RATE_LIMIT_PREFIX + "subject:" + subject + ":" + method,
                        rateLimit.maxRequests(), windowSeconds)) {
                    log.warn("[限流] 主体维度触发 uri={} subject={} method={}",
                            request.getRequestURI(), subject, method);
                    throw new BusinessException("429000", rateLimit.message());
                }
            }
        }

        return joinPoint.proceed();
    }

    /** 计数并判断是否超限；Redis 异常时返回 false（放行），保证限流故障不影响主流程 */
    private boolean exceeded(String key, int max, long windowSeconds) {
        try {
            // 注意：本项目的 RedisTemplate value 序列化器是 GenericJackson2JsonRedisSerializer，
            // 脚本参数会走该序列化器。这里显式传字符串，避免把数字交给 JSON 序列化器产生歧义。
            Long count = redisUtil.execute(RATE_LIMIT_SCRIPT, List.of(key),
                    String.valueOf(windowSeconds));
            return count != null && count > max;
        } catch (Exception e) {
            log.warn("[限流] Redis 异常，本次放行 key={} 原因={}", key, e.getMessage());
            return false;
        }
    }

    private long toSeconds(RateLimit rateLimit) {
        long seconds = rateLimit.timeUnit().toSeconds(rateLimit.timeWindow());
        return Math.max(seconds, 1L);
    }

    private String resolveMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    /**
     * 解析业务主体标识：优先已登录身份，其次请求参数中的账户字段。
     */
    private String resolveSubject(ProceedingJoinPoint joinPoint) {
        String account = LoginContext.getAccount();
        if (account != null && !account.isEmpty()) {
            return "acct:" + account;
        }
        Long userId = LoginContext.getUserId();
        if (userId != null) {
            return "user:" + userId;
        }
        Long staffId = LoginContext.getStaffId();
        if (staffId != null) {
            return "staff:" + staffId;
        }
        // 未登录场景（登录、注册、发码、重置密码）：从 DTO 参数中取账户字段
        for (Object arg : joinPoint.getArgs()) {
            String value = extractSubjectField(arg);
            if (value != null && !value.isEmpty()) {
                return "acct:" + value;
            }
        }
        return null;
    }

    private String extractSubjectField(Object arg) {
        if (arg == null || arg instanceof CharSequence) {
            return null;
        }
        for (String fieldName : SUBJECT_FIELDS) {
            try {
                Field field = arg.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(arg);
                if (value instanceof String s && !s.isBlank()) {
                    return s;
                }
            } catch (NoSuchFieldException | IllegalAccessException | RuntimeException e) {
                // 该参数没有这个字段，或不可访问：跳过
            }
        }
        return null;
    }

    /**
     * 取客户端 IP。
     *
     * <p>只有在明确部署于可信反向代理之后时才会采信 {@code X-Forwarded-For} / {@code X-Real-IP}；
     * 否则代理头完全由客户端控制，采信它等于放弃限流。
     */
    private String getClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr();
    }
}
