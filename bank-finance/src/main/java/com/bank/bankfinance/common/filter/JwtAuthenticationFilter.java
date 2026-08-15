package com.bank.bankfinance.common.filter;

import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 无需登录即可访问的公开路径（登录/注册/发码/重置密码/行情） */
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/account/login", "/account/register", "/account/sendCode", "/account/resetPassword",
            "/user/login", "/user/register",
            "/support/login", "/support/register",
            "/stock/list", "/stock/quote", "/stock/provider"
    );

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (Boolean.TRUE.equals(redisUtil.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
                    log.warn("Token 已失效(黑名单), URI={}", request.getRequestURI());
                    reject(response);
                    return;
                }
            } catch (Exception e) {
                log.warn("黑名单查询异常, URI={}, 降级放行: {}", request.getRequestURI(), e.getMessage());
            }
            if (!PUBLIC_PATHS.contains(request.getRequestURI())) {
                try {
                    String subject = jwtUtil.parseToken(token);
                    if (subject.startsWith("U_")) {
                        LoginContext.setUserId(Long.parseLong(subject.substring(2)));
                        log.info("[JWT] URI={} 解析用户身份 subject={} userId={}", request.getRequestURI(), subject, LoginContext.getUserId());
                    } else if (subject.startsWith("S_")) {
                        LoginContext.setStaffId(Long.parseLong(subject.substring(2)));
                        log.info("[JWT] URI={} 解析客服身份 subject={} staffId={}", request.getRequestURI(), subject, LoginContext.getStaffId());
                    } else {
                        LoginContext.setAccount(subject);
                        log.info("[JWT] URI={} 解析卡号身份 subject={} account={}", request.getRequestURI(), subject, LoginContext.getAccount());
                    }
                } catch (Exception e) {
                    log.warn("Token 解析失败, URI={}, 错误: {}", request.getRequestURI(), e.getMessage());
                    reject(response);
                    return;
                }
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoginContext.clear();
        }
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"401000\",\"msg\":\"登录已失效，请重新登录\"}");
    }
}