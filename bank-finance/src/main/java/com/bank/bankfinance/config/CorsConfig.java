package com.bank.bankfinance.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置。
 *
 * <p>修复记录：原实现为 {@code allowedOriginPatterns("*")} 搭配
 * {@code allowCredentials(true)}，等价于「任意站点都可以携带用户凭据发起跨域请求」，
 * 配合本项目的 JWT 存放在客户端，等于放弃了同源策略保护。
 *
 * <p>当前实现改为从配置读取白名单，默认仅放行本地开发常用的前端端口。
 * 生产部署请通过 {@code app.cors.allowed-origins} 显式列出前端域名。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:8080,http://127.0.0.1:5173,http://127.0.0.1:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
