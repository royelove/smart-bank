package com.bank.bankfinance.config;

import com.bank.bankfinance.common.constant.ResponseCode;
import com.bank.bankfinance.common.filter.JwtAuthenticationFilter;
import com.bank.bankfinance.common.result.Result;
import com.bank.bankfinance.common.util.AuthResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * 授权规则集中定义处。
 *
 * <p>修复记录：原配置为 {@code anyRequest().permitAll()}，配合 JwtAuthenticationFilter
 * 只在「携带 Authorization 头」时才工作的实现，使得**完全不带 token 的请求可以访问任意接口**
 * （实测 {@code /account/transfer}、{@code /support/admin/idcard/list} 均 200 并进入 Controller），
 * 而带非法 token 的请求反而被 401 —— 认证事实上退化成「客户端自愿参加」。
 *
 * <p>当前策略：<b>默认拒绝 + 显式白名单</b>。公开路径在此单点维护，
 * {@link JwtAuthenticationFilter} 不再自行维护一份会漂移的副本。
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    /** 无需登录即可访问的公开路径（登录 / 注册 / 发码 / 重置密码 / 行情 / 接口文档） */
    private static final String[] PUBLIC_PATHS = {
            "/account/login", "/account/register", "/account/sendCode", "/account/resetPassword",
            "/user/login", "/user/register",
            "/support/login", "/support/register",
            "/stock/list", "/stock/quote", "/stock/provider", "/stock/kline/**",
            // 接口文档：knife4j / springdoc
            "/doc.html", "/webjars/**", "/swagger-ui.html", "/swagger-ui/**",
            "/swagger-resources/**", "/v3/api-docs/**",
            "/favicon.ico", "/error"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                    // CORS 预检请求不携带凭据，必须先于鉴权放行
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(PUBLIC_PATHS).permitAll()
                    // 客服后台：必须持有客服身份（S_ 前缀 token）
                    .requestMatchers("/support/admin/**").hasRole("STAFF")
                    // 其余一律需要有效身份
                    .anyRequest().authenticated())
            /*
             * 修复记录：授权改成「默认拒绝」之后，必须同时给出**失败出口**。
             * 此前没有配置 exceptionHandling，Spring Security 用的是默认的
             * Http403ForbiddenEntryPoint：未登录访问受保护接口会返回「403 + 空响应体」，
             * 前端既拿不到 code 也分不清「未登录 / 无权限」，登录跳转逻辑直接失效。
             * 现在：未登录 → 401 + {code:700002}，已登录但角色不足 → 403 + {code:700001}，
             * 结构与 GlobalExceptionHandler 的输出完全一致。
             */
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(this::onUnauthenticated)
                    .accessDeniedHandler(this::onAccessDenied))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 未认证（含匿名）：401 + 统一 JSON */
    void onUnauthenticated(HttpServletRequest request, HttpServletResponse response,
                           AuthenticationException authException) throws IOException {
        AuthResponseWriter.unauthorized(response, objectMapper, Result.fail(ResponseCode.NO_LOGIN));
    }

    /** 已认证但角色不足（例如非客服访问 /support/admin/**）：403 + 统一 JSON */
    void onAccessDenied(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        AuthResponseWriter.forbidden(response, objectMapper, Result.fail(ResponseCode.NO_ADMIN_PERMISSION));
    }

    /**
     * 关闭 JwtAuthenticationFilter 的 Servlet 容器自动注册。
     *
     * <p>该类带 {@code @Component} 且继承 {@code OncePerRequestFilter}，Spring Boot 会把它
     * 额外注册为一个全局 Servlet Filter；同时它又被 {@code addFilterBefore} 加入 Spring Security
     * 过滤链，导致同一请求上被调用两次。这里禁用自动注册，只保留 Security 链中的那一份。
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
