package com.example.paymentservice.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.util.Base64;

public class TossPaymentsConfig {

    @Value("${toss.payments.api.secret-key}")
    private String secretKey;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Basic Auth: base64("{API_SECRET_KEY}:")
            String auth = secretKey + ":";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            requestTemplate.header("Authorization", "Basic " + encodedAuth);
            requestTemplate.header("Content-Type", "application/json");
        };
    }
}
