package com.bank.bankfinance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
/*
SpringMVC全局跨域配置类，解决浏览器同源策略带来的跨域请求报错
用于保证每种浏览器都能访问
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 所有接口
                .allowedOriginPatterns("*") // 允许所有前端域名（生产环境要写死）
                .allowedMethods("*")        // 允许所有方法（GET/POST等）
                .allowedHeaders("*")        // 允许所有请求头
                .allowCredentials(true);    // 允许携带 Cookie/Token
    }
}
