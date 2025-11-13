package com.i2i.user_management.Integration.Client;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateClient {

    BigDecimal getRateToInr(String currency);

    Map<String, String> getAllCurrencies();

}
