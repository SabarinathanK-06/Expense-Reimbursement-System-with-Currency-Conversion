package com.i2i.user_management.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing total approved expense per employee,
 * including their total amount and converted INR equivalent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeExpenseSummaryDto {

    private String employeeId;
    private String employeeName;
    private String currency;
    private BigDecimal totalAmount;
    private BigDecimal totalApprovedInInr;

    public EmployeeExpenseSummaryDto(String employeeId, String employeeName, String currency, BigDecimal totalAmount) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.currency = currency;
        this.totalAmount = totalAmount;
    }
}
