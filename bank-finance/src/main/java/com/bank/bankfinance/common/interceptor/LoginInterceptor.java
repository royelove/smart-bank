package com.bank.bankfinance.common.interceptor;

import com.bank.bankfinance.common.util.JwtUtil;
import com.bank.bankfinance.common.util.LoginContext;
import com.bank.bankfinance.common.util.RedisUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (Boolean.TRUE.equals(redisUtil.hasKey(TOKEN_BLACKLIST_PREFIX + token))) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return false;
                }
                String accountNumber = jwtUtil.parseToken(token);
                LoginContext.setAccount(accountNumber);
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginContext.clear();
    }
}
