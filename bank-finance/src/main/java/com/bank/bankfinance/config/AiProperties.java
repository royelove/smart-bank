package com.bank.bankfinance.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String apiKey = "";
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com";
}
