package com.i2i.user_management.Dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing an expense record with all key details,
 * including INR conversion and status.
 */
@Data
public class ExpenseResponseDto {

    private UUID id;

    private String title;

    private String description;

    private LocalDate expenseDate;

    private BigDecimal amount;

    private String currency;

    private BigDecimal amountInInr;

    private String receiptUrl;

    private String status;

    private String requestedBy;

    private String approvedByName;

    private String rejectionReason;
}
