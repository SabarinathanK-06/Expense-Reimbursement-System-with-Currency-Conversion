package com.i2i.user_management.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for connecting to the External API.
 */
@Configuration
public class ExchangeRateConfig {

    @Value("${fast.forex.base-url}")
    private String BASE_URL;

    @Bean
    public WebClient exchangeWebClient() {
        return WebClient.builder()
                .baseUrl(BASE_URL)
                .build();
    }
}