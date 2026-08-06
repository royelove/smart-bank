package com.bank.bankfinance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
/*
    把配置文件application.properties里面包括ai的所有字段全部注入到这个里面
 */
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String apiKey = "";
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com";
}
