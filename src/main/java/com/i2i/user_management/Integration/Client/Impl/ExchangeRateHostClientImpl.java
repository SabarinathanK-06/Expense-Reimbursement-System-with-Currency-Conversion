package com.i2i.user_management.Integration.Client.Impl;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Exception.ApplicationException;
import com.i2i.user_management.Exception.ExternalServiceException;
import com.i2i.user_management.Integration.Client.ExchangeRateClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * Client for fetching exchange rates.
 */
@Component
public class ExchangeRateHostClientImpl implements ExchangeRateClient {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateHostClientImpl.class);

    private final WebClient webClient;

    @Value("${fast.forex.api.key}")
    private String API_KEY;

    public ExchangeRateHostClientImpl(WebClient exchangeWebClient) {
        this.webClient = exchangeWebClient;
    }


    @Override
    public BigDecimal getRateToInr(String currency) {
        if (UMSConstants.INR.equalsIgnoreCase(currency)) {
            return BigDecimal.ONE;
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/fetch-one")
                            .queryParam("from", currency.toUpperCase())
                            .queryParam("to", UMSConstants.INR)
                            .queryParam("api_key", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono((Map.class))
                    .block();

            if (response == null || !response.containsKey("result")) {
                log.warn("Unexpected response from exchange API for {}", currency);
                throw new RuntimeException("Invalid response from exchange rate API");
            }

            Map<String, Double> result = (Map<String, Double>) response.get("result");
            Double inrRate = result.get(UMSConstants.INR);

            if (inrRate == null) {
                throw new RuntimeException("INR rate not found in response");
            }

            log.info("Fetched rate for {}: {}", currency, inrRate);
            return BigDecimal.valueOf(inrRate);

        } catch (WebClientResponseException e) {
            log.warn("API returned error: {}", e.getResponseBodyAsString());
            throw new RuntimeException("External API error: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to fetch exchange rate for {}", currency, e);
            throw new RuntimeException("Unable to fetch exchange rate");
        }
    }

    /**
     * Fetches all supported currencies with their codes and names.
     */
    public Map<String, String> getAllCurrencies() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/currencies")
                            .queryParam("api_key", API_KEY)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("currencies")) {
                log.warn("Unexpected response when fetching all currencies: {}", response);
                return Collections.emptyMap();
            }

            Map<String, String> currencies = (Map<String, String>) response.get("currencies");
            log.info("Fetched {} currencies successfully", currencies.size());
            return currencies;

        } catch (WebClientResponseException e) {
            log.warn("Currency list API returned error: {}", e.getResponseBodyAsString());
            throw new ExternalServiceException("External API error while fetching currency list: " , e);
        } catch (Exception e) {
            log.error("Failed to fetch currency list", e);
            throw new ApplicationException("Unable to fetch currency list");
        }
    }

}