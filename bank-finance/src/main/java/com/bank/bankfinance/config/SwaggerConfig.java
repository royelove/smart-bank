package com.bank.bankfinance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("银行账户管理系统 API")
                        .description("基于 SpringBoot + MyBatis-Plus + SQL Server 的银行账户管理后端系统")
                        .version("1.0.0"));
    }
}
