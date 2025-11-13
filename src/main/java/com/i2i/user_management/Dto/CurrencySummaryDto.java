package com.i2i.user_management.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CurrencySummaryDto {

    private String currency;

    private BigDecimal totalOriginalAmount;

    private BigDecimal totalAmountInInr;

    public CurrencySummaryDto(String currency, BigDecimal totalOriginalAmount) {
        this.currency = currency;
        this.totalOriginalAmount = totalOriginalAmount;
    }

}
