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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

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
                    log.warn("Token 已被加入黑名单");
                } else {
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
                }
            } catch (Exception e) {
                log.warn("Token 解析失败, URI={}, 错误: {}", request.getRequestURI(), e.getMessage());
            }
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoginContext.clear();
        }
    }
}
