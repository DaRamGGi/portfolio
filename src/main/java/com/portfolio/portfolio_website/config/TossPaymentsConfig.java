package com.portfolio.portfolio_website.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@ConfigurationProperties(prefix = "tosspayments")
@Getter
@Setter
public class TossPaymentsConfig {
    
    private String clientKey;
    private String secretKey;
    private String successUrl;
    private String failUrl;
    private Api api;
    
    @Getter
    @Setter
    public static class Api {
        private String baseUrl;
        private String confirmUrl;
        private String paymentUrl;
    }
    
    @Bean
    public WebClient tossPaymentsWebClient() {
        return WebClient.builder()
                .baseUrl(api.getBaseUrl())
                .defaultHeader("Authorization", "Basic " + 
                    java.util.Base64.getEncoder().encodeToString((secretKey + ":").getBytes()))
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
