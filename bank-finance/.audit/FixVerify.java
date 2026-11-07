import com.bank.bankfinance.common.aspect.RateLimitAspect;
import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.exception.BusinessException;
import com.bank.bankfinance.common.exception.GlobalExceptionHandler;
import com.bank.bankfinance.common.filter.JwtAuthenticationFilter;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import com.bank.bankfinance.dto.trade.TransferDTO;
import com.bank.bankfinance.mapper.AccountMapper;
import com.bank.bankfinance.mapper.FinancialProductMapper;
import com.bank.bankfinance.mapper.PositionMapper;
import com.bank.bankfinance.service.AccountService;
import com.bank.bankfinance.service.impl.AccountServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 修复验证脚手架（不需要 MySQL / Redis / RabbitMQ）。
 * 覆盖：参数校验、异常到 HTTP 状态码的映射、Mapper SQL 守卫、账户缓存延迟失效、
 * JWT 过滤器的新职责边界、限流 Lua 脚本的原子性。
 */
public class FixVerify {

    static int passed = 0;
    static int failed = 0;

    static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.printf("  [PASS] %s%n", name);
        } else {
            failed++;
            System.out.printf("  [FAIL] %s  <-- %s%n", name, detail);
        }
    }

    static void checkEq(String name, Object expected, Object actual) {
        check(name, expected == null ? actual == null : expected.equals(actual),
                "expected=" + expected + " actual=" + actual);
    }

    static void setField(Object target, Class<?> type, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // ---------------------------------------------------------------- 1. 参数校验

    static TransferDTO transferDTO(String amount) throws Exception {
        Constructor<TransferDTO> c = TransferDTO.class.getDeclaredConstructor();
        c.setAccessible(true);
        TransferDTO dto = c.newInstance();
        dto.setInNumber("6222020000000002");
        dto.setOutNumber("6222020000000001");
        dto.setPassword("x");
        dto.setRequestId("req-1");
        if (amount != null) {
            dto.setAmount(new BigDecimal(amount));
        }
        return dto;
    }

    static void verifyValidation() throws Exception {
        System.out.println("=== 1. TransferDTO 参数校验（原 HV000030 导致转账整体不可用） ===");
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        Set<ConstraintViolation<TransferDTO>> ok = validator.validate(transferDTO("100.00"));
        check("合法金额 100.00 通过校验", ok.isEmpty(), String.valueOf(ok));

        Set<ConstraintViolation<TransferDTO>> nullAmount = validator.validate(transferDTO(null));
        check("金额为 null 被拒绝（而不是抛 UnexpectedTypeException）", nullAmount.size() == 1,
                "violations=" + nullAmount.size());
        check("拒绝原因是业务文案而非校验器异常",
                nullAmount.stream().anyMatch(v -> "转账金额不能为空".equals(v.getMessage())),
                String.valueOf(nullAmount));

        Set<ConstraintViolation<TransferDTO>> zero = validator.validate(transferDTO("0.00"));
        check("金额 0.00 被 @DecimalMin 拒绝", zero.size() == 1, String.valueOf(zero));
    }

    // ---------------------------------------------------------------- 2. 异常 -> HTTP 状态码

    static void verifyHttpStatusMapping() throws Exception {
        System.out.println("=== 2. GlobalExceptionHandler 的 HTTP 状态码映射 ===");
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        setField(handler, GlobalExceptionHandler.class, "exposeHttpStatus", true);

        checkEq("限流 429000 -> 429", 429,
                handler.handleBusinessException(new BusinessException("429000", "请求过于频繁"))
                        .getStatusCode().value());
        checkEq("未登录 NO_LOGIN -> 401", 401,
                handler.handleBusinessException(new BusinessException(ResponseCode.NO_LOGIN))
                        .getStatusCode().value());
        checkEq("越权 NO_ADMIN_PERMISSION -> 403", 403,
                handler.handleBusinessException(new BusinessException(ResponseCode.NO_ADMIN_PERMISSION))
                        .getStatusCode().value());
        checkEq("客服注册校验失败 -> 403", 403,
                handler.handleBusinessException(new BusinessException(ResponseCode.STAFF_REGISTER_FORBIDDEN))
                        .getStatusCode().value());
        checkEq("普通业务错误(密码错误) 仍是 200（保持前端契约）", 200,
                handler.handleBusinessException(new BusinessException(ResponseCode.PASSWORD_INVALID))
                        .getStatusCode().value());
        checkEq("未捕获运行时异常 -> 500", 500,
                handler.handleRuntimeException(new IllegalStateException("boom")).getStatusCode().value());

        setField(handler, GlobalExceptionHandler.class, "exposeHttpStatus", false);
        checkEq("app.error.expose-http-status=false 时可回退为全 200", 200,
                handler.handleBusinessException(new BusinessException("429000", "过于频繁"))
                        .getStatusCode().value());
    }

    // ---------------------------------------------------------------- 3. Mapper SQL 守卫

    static String updateSql(Class<?> mapper, String method) throws Exception {
        for (Method m : mapper.getDeclaredMethods()) {
            if (m.getName().equals(method)) {
                Update u = m.getAnnotation(Update.class);
                if (u != null) {
                    return String.join(" ", u.value()).replaceAll("\\s+", " ");
                }
            }
        }
        return "<not found: " + method + ">";
    }

    static void verifySqlGuards() throws Exception {
        System.out.println("=== 3. Mapper 条件更新的守卫与 GREATEST 掩盖问题 ===");
        String settleBuy = updateSql(AccountMapper.class, "settleBuy");
        check("settleBuy 守卫冻结额", settleBuy.contains("frozen_balance >= #{freezeAmount}"), settleBuy);
        check("settleBuy 守卫余额", settleBuy.contains("account_balance >= #{actualAmount}"), settleBuy);
        check("settleBuy 不再用 GREATEST 掩盖超额释放", !settleBuy.contains("GREATEST"), settleBuy);

        String unfreeze = updateSql(AccountMapper.class, "unfreezeBalance");
        check("unfreezeBalance 守卫 frozen_balance >= 释放额",
                unfreeze.contains("frozen_balance >= #{amount}"), unfreeze);
        check("unfreezeBalance 不再用 GREATEST", !unfreeze.contains("GREATEST"), unfreeze);

        String deduct = updateSql(AccountMapper.class, "deductAvailableBalance");
        check("出账按可用资金（余额-冻结）守卫",
                deduct.contains("account_balance - frozen_balance >= #{amount}"), deduct);

        String reserve = updateSql(FinancialProductMapper.class, "reserveAmount");
        check("理财额度是数据库层原子扣减",
                reserve.contains("remaining_amount = remaining_amount - #{amount}"), reserve);
        check("理财额度不足时返回 0 行", reserve.contains("remaining_amount >= #{amount}"), reserve);

        String unfreezeShares = updateSql(PositionMapper.class, "unfreezeShares");
        check("unfreezeShares 守卫 frozen_quantity", unfreezeShares.contains("frozen_quantity >= #{qty}"), unfreezeShares);
        check("unfreezeShares 不再用 GREATEST", !unfreezeShares.contains("GREATEST"), unfreezeShares);

        String settleSell = updateSql(PositionMapper.class, "settleSell");
        check("settleSell 同时守卫持仓与冻结股数",
                settleSell.contains("quantity >= #{qty}") && settleSell.contains("frozen_quantity >= #{qty}"),
                settleSell);
        check("settleSell 不再用 GREATEST", !settleSell.contains("GREATEST"), settleSell);
    }

    // ---------------------------------------------------------------- 4. 缓存延迟失效

    static void verifyDeferredCacheEviction() throws Exception {
        System.out.println("=== 4. AccountServiceImpl.evictAccountCache 的事务后失效 ===");
        AccountServiceImpl impl = new AccountServiceImpl();
        AtomicInteger deletes = new AtomicInteger();
        RedisUtil stub = new RedisUtil() {
            @Override
            public Boolean delete(String key) {
                deletes.incrementAndGet();
                return true;
            }
        };
        setField(impl, AccountServiceImpl.class, "redisUtil", stub);

        impl.evictAccountCache("6222020000000001");
        checkEq("非事务上下文：立即删除缓存", 1, deletes.get());

        deletes.set(0);
        TransactionSynchronizationManager.initSynchronization();
        try {
            impl.evictAccountCache("6222020000000001");
            checkEq("事务上下文中：提交前不删除（否则并发读会把旧值回填）", 0, deletes.get());
            for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
                s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
            checkEq("事务完成后：缓存被删除", 1, deletes.get());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ---------------------------------------------------------------- 5. JWT 过滤器

    static JwtUtil jwtUtil() throws Exception {
        JwtUtil util = new JwtUtil();
        setField(util, JwtUtil.class, "secret", "smartBankJwtSecretKeyAtLeast32BytesLong!2026");
        setField(util, JwtUtil.class, "expiration", 86400000L);
        return util;
    }

    static class Probe {
        boolean chainReached;
        int status;
        String body = "";
        Authentication authenticationOnChain;
        Long userIdOnChain;
        String accountOnChain;
        Long staffIdOnChain;
    }

    static Probe probe(JwtUtil util, boolean blacklisted, String uri, String authHeader) throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        setField(filter, JwtAuthenticationFilter.class, "jwtUtil", util);
        setField(filter, JwtAuthenticationFilter.class, "objectMapper",
                new com.fasterxml.jackson.databind.ObjectMapper());
        setField(filter, JwtAuthenticationFilter.class, "redisUtil", new RedisUtil() {
            @Override
            public Boolean hasKey(String key) {
                return blacklisted;
            }
        });

        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRequestURI(uri);
        if (authHeader != null) {
            request.addHeader("Authorization", authHeader);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        Probe p = new Probe();
        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest r, ServletResponse s) {
                p.chainReached = true;
                // 过滤器 finally 会清理上下文，必须在链内快照
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                p.authenticationOnChain = auth;
                p.userIdOnChain = LoginContext.getUserId();
                p.accountOnChain = LoginContext.getAccount();
                p.staffIdOnChain = LoginContext.getStaffId();
            }
        };
        filter.doFilter(request, response, chain);
        p.status = response.getStatus();
        p.body = response.getContentAsString();
        return p;
    }

    static void verifyJwtFilter() throws Exception {
        System.out.println("=== 5. JwtAuthenticationFilter 的新职责边界 ===");
        JwtUtil util = jwtUtil();

        Probe anonymous = probe(util, false, "/account/transfer", null);
        check("无 token：不再由过滤器放行/拦截，而是进入 Spring Security 授权链",
                anonymous.chainReached, "chainReached=false");
        check("无 token：SecurityContext 中没有任何身份",
                anonymous.authenticationOnChain == null, String.valueOf(anonymous.authenticationOnChain));
        check("无 token：LoginContext 也没有身份（否则业务层会拿到别人的身份）",
                anonymous.accountOnChain == null && anonymous.userIdOnChain == null
                        && anonymous.staffIdOnChain == null,
                account(anonymous));

        String accountToken = util.generateToken("6222020000000001");
        Probe account = probe(util, false, "/account/getAccountVO", "Bearer " + accountToken);
        check("合法卡号 token：写入 SecurityContext 且带 ROLE_USER",
                account.authenticationOnChain != null
                        && account.authenticationOnChain.getAuthorities().toString().contains("ROLE_USER"),
                String.valueOf(account.authenticationOnChain));
        checkEq("合法卡号 token：LoginContext.account 被填充", "6222020000000001", account.accountOnChain);

        String userToken = util.generateToken("U_7");
        Probe user = probe(util, false, "/user/profile", "Bearer " + userToken);
        checkEq("U_ 前缀 token：解析为 userId", 7L, user.userIdOnChain);

        String staffToken = util.generateToken("S_42");
        Probe staff = probe(util, false, "/support/admin/idcard/list", "Bearer " + staffToken);
        boolean hasBoth = staff.authenticationOnChain != null
                && staff.authenticationOnChain.getAuthorities().toString().contains("ROLE_STAFF")
                && staff.authenticationOnChain.getAuthorities().toString().contains("ROLE_USER");
        check("S_ 前缀 token：同时具备 ROLE_STAFF 与 ROLE_USER（/support/admin/** 才放得进去）",
                hasBoth, String.valueOf(staff.authenticationOnChain));
        checkEq("S_ 前缀 token：LoginContext.staffId 被填充", 42L, staff.staffIdOnChain);

        Probe invalid = probe(util, false, "/account/transfer", "Bearer not.a.real.token");
        check("非法 token：401 且不进业务链", invalid.status == 401 && !invalid.chainReached,
                "status=" + invalid.status + " chainReached=" + invalid.chainReached);
        check("非法 token：响应体带 401000", invalid.body.contains("401000"), invalid.body);

        Probe blacklisted = probe(util, true, "/account/transfer", "Bearer " + accountToken);
        check("已登出（黑名单）token：401", blacklisted.status == 401 && !blacklisted.chainReached,
                "status=" + blacklisted.status);

        check("过滤后上下文被清理，避免 Tomcat 线程复用导致身份泄漏",
                SecurityContextHolder.getContext().getAuthentication() == null
                        && LoginContext.getAccount() == null && LoginContext.getUserId() == null,
                "leftover identity");
    }

    static String account(Probe p) {
        return "account=" + p.accountOnChain + " userId=" + p.userIdOnChain + " staffId=" + p.staffIdOnChain;
    }

    // ---------------------------------------------------------------- 6. 限流脚本与主体解析

    static void verifyRateLimit() throws Exception {
        System.out.println("=== 6. 限流的原子性（Lua）与主体解析顺序 ===");
        Field scriptField = RateLimitAspect.class.getDeclaredField("RATE_LIMIT_SCRIPT");
        scriptField.setAccessible(true);
        @SuppressWarnings("unchecked")
        RedisScript<Long> script = (RedisScript<Long>) scriptField.get(null);
        String lua = script.getScriptAsString().replaceAll("\\s+", " ");
        check("使用 Lua 脚本而非 INCR+EXPIRE 两步", lua.contains("redis.call('INCR'"), lua);
        check("首次自增即设置 TTL（避免 key 永久不过期把账号锁死）",
                lua.contains("redis.call('EXPIRE'"), lua);
        check("TTL 丢失时会补设（防御历史上已存在的无 TTL key）", lua.contains("TTL"), lua);

        Field fieldsField = RateLimitAspect.class.getDeclaredField("SUBJECT_FIELDS");
        fieldsField.setAccessible(true);
        String[] fields = (String[]) fieldsField.get(null);
        java.util.List<String> order = java.util.Arrays.asList(fields);
        check("付款方 outNumber 优先于收款方 inNumber（否则会给收款人限流）",
                order.indexOf("outNumber") >= 0 && order.indexOf("inNumber") >= 0
                        && order.indexOf("outNumber") < order.indexOf("inNumber"),
                order.toString());
    }

    // ---------------------------------------------------------------- 7. 服务接口契约

    static void verifyServiceContract() throws Exception {
        System.out.println("=== 7. 服务接口契约（失败必须能被调用方发现） ===");
        check("releaseBuyFunds 返回 boolean", AccountService.class
                .getMethod("releaseBuyFunds", String.class, BigDecimal.class).getReturnType() == boolean.class, "");
        check("settleBuyFunds 返回 boolean", AccountService.class
                .getMethod("settleBuyFunds", String.class, BigDecimal.class, BigDecimal.class)
                .getReturnType() == boolean.class, "");
        check("对外暴露 evictAccountCache（理财购买/赎回也会改余额）",
                AccountService.class.getMethod("evictAccountCache", String.class) != null, "");

        Method settleHolding = null;
        for (Method m : Class.forName("com.bank.bankfinance.service.impl.FinancialServiceImpl")
                .getDeclaredMethods()) {
            if (m.getName().equals("settleHolding")) {
                settleHolding = m;
            }
        }
        check("settleHolding(FinancialHolding, LocalDateTime) 存在且为私有实例方法",
                settleHolding != null && settleHolding.getParameterCount() == 2
                        && settleHolding.getReturnType() == void.class
                        && java.lang.reflect.Modifier.isPrivate(settleHolding.getModifiers()), "");
    }

    // ---------------------------------------------------------------- 8. 鉴权失败的响应出口

    static void verifyAuthFailureResponses() throws Exception {
        System.out.println("=== 8. 鉴权失败的响应出口（默认 403 空 body 会让前端丢语义） ===");
        Class<?> cfg = Class.forName("com.bank.bankfinance.config.SecurityConfig");
        Object instance = cfg.getDeclaredConstructor().newInstance();
        setField(instance, cfg, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());

        Method unauthenticated = cfg.getDeclaredMethod("onUnauthenticated",
                jakarta.servlet.http.HttpServletRequest.class,
                jakarta.servlet.http.HttpServletResponse.class,
                org.springframework.security.core.AuthenticationException.class);
        unauthenticated.setAccessible(true);
        MockHttpServletResponse r1 = new MockHttpServletResponse();
        unauthenticated.invoke(instance, new MockHttpServletRequest("GET", "/account/getAccountVO"), r1, null);
        String body1 = r1.getContentAsString();
        checkEq("未认证 -> 401", 401, r1.getStatus());
        check("未认证响应体是带 code 的 JSON（前端据此跳登录页）",
                body1.contains("\"code\":\"700002\""), body1);
        check("未认证响应体结构与其他接口一致（含 data 字段）", body1.contains("\"data\""), body1);

        Method denied = cfg.getDeclaredMethod("onAccessDenied",
                jakarta.servlet.http.HttpServletRequest.class,
                jakarta.servlet.http.HttpServletResponse.class,
                org.springframework.security.access.AccessDeniedException.class);
        denied.setAccessible(true);
        MockHttpServletResponse r2 = new MockHttpServletResponse();
        denied.invoke(instance, new MockHttpServletRequest("GET", "/support/admin/idcard/list"), r2, null);
        String body2 = r2.getContentAsString();
        checkEq("已认证但角色不足 -> 403", 403, r2.getStatus());
        check("无权限响应体 code=700001", body2.contains("\"code\":\"700001\""), body2);

        Probe invalid = probe(jwtUtil(), false, "/account/transfer", "Bearer not.a.real.token");
        check("非法 token 的响应体也统一为 Result 结构（含 data 字段）",
                invalid.body.contains("\"code\":\"401000\"") && invalid.body.contains("\"data\""), invalid.body);
    }

    public static void main(String[] args) throws Exception {
        verifyValidation();
        verifyHttpStatusMapping();
        verifySqlGuards();
        verifyDeferredCacheEviction();
        verifyRateLimit();
        verifyJwtFilter();
        verifyServiceContract();
        verifyAuthFailureResponses();
        System.out.printf("%n合计: PASS=%d FAIL=%d%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }
}
