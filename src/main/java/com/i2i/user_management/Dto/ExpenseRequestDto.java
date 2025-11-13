package com.i2i.user_management.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.i2i.user_management.annotation.NotFutureDate;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO used for submitting a new expense.
 */
@Data
public class ExpenseRequestDto {

    @NotNull
    private String title;

    @NotNull
    private String description;

    @NotNull
    @NotFutureDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expenseDate;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String currency;

    @NotNull
    private String receiptUrl;

}
