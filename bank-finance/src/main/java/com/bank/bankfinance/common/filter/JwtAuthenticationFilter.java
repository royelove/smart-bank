package com.bank.bankfinance.common.filter;

import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.common.util.AuthResponseWriter;
import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器。
 *
 * <p>职责边界（修复后的设计）：本过滤器<b>只负责「是否已认证」</b>——把合法 token 解析成
 * Spring Security 的 {@code Authentication} 并写入 {@code SecurityContextHolder}，
 * 同时继续填充业务侧仍在使用的手写 {@link LoginContext}。<b>「是否允许访问」由
 * SecurityConfig 的授权规则统一决定</b>，不再由本类维护一份会漂移的公开路径白名单。
 *
 * <p>修复记录：原实现把整段鉴权逻辑包在 {@code if (authHeader != null && ...)} 之内，
 * 并且在解析成功后<b>既不写入 SecurityContextHolder 也不拒绝无 token 的请求</b>，
 * 而 SecurityConfig 又是 {@code anyRequest().permitAll()}。结果是：不携带 Authorization
 * 头的请求可以直达任意受保护接口（实测 {@code /account/transfer}、
 * {@code /support/admin/idcard/list} 均返回 200 并进入 Controller）；反而携带非法 token
 * 的请求会被 401 拦截——行为与预期完全相反。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 普通用户（卡号身份 / U_ 用户身份） */
    public static final String ROLE_USER = "ROLE_USER";
    /** 客服人员（S_ 身份），/support/admin/** 要求该角色 */
    public static final String ROLE_STAFF = "ROLE_STAFF";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    /** 新增：与 GlobalExceptionHandler 共用同一个 Jackson 配置，保证响应体结构一致 */
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                if (isBlacklisted(token, request)) {
                    reject(response);
                    return;
                }
                try {
                    String subject = jwtUtil.parseToken(token);
                    authenticate(request, subject);
                } catch (Exception e) {
                    log.warn("Token 解析失败, URI={}, 错误: {}", request.getRequestURI(), e.getMessage());
                    reject(response);
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // Tomcat 线程会被复用，必须清理两个上下文，否则身份会泄漏到后续请求
            LoginContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 把 token 主体解析为已认证身份。
     *
     * <p>注意：这里不再判断请求路径是否为「公开路径」。公开与否属于授权决策，
     * 已收敛到 SecurityConfig 一处维护。
     */
    private void authenticate(HttpServletRequest request, String subject) {
        List<GrantedAuthority> authorities;
        String principal;

        if (subject.startsWith("U_")) {
            Long userId = Long.parseLong(subject.substring(2));
            LoginContext.setUserId(userId);
            principal = "user:" + userId;
            authorities = List.of(new SimpleGrantedAuthority(ROLE_USER));
        } else if (subject.startsWith("S_")) {
            Long staffId = Long.parseLong(subject.substring(2));
            LoginContext.setStaffId(staffId);
            principal = "staff:" + staffId;
            // 客服同时具备普通用户角色，便于复用面向前台账户的只读接口
            authorities = List.of(new SimpleGrantedAuthority(ROLE_STAFF),
                                  new SimpleGrantedAuthority(ROLE_USER));
        } else {
            LoginContext.setAccount(subject);
            principal = "account:" + subject;
            authorities = List.of(new SimpleGrantedAuthority(ROLE_USER));
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[JWT] URI={} 已认证 subject={} authorities={}",
                request.getRequestURI(), subject, authorities);
    }

    /**
     * 退出登录黑名单校验。
     *
     * <p>刻意保持「Redis 异常时降级放行」：token 的签名与有效期仍在本方法之后独立校验，
     * 身份不会被伪造；把一次 Redis 抖动升级为全站无法访问并不划算。此处仅记录告警。
     */
    private boolean isBlacklisted(String token, HttpServletRequest request) {
        try {
            return Boolean.TRUE.equals(redisUtil.hasKey(TOKEN_BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            log.warn("黑名单查询异常, URI={}, 降级放行(签名仍会校验): {}",
                    request.getRequestURI(), e.getMessage());
            return false;
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        /*
         * 修复记录：原为手写字符串拼接的 JSON（"{...}"），与 GlobalExceptionHandler 的
         * Result 结构不一致（没有 data 字段）。统一走 AuthResponseWriter 输出同样的结构。
         */
        AuthResponseWriter.unauthorized(response, objectMapper,
                Result.fail(ResponseCode.TOKEN_INVALID));
    }
}
