package com.i2i.user_management.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO used by Finance Admin to approve or reject an expense.
 */
@Data
public class ExpenseApprovalDto {

    @NotNull
    private String status;

    private String reason;
}
