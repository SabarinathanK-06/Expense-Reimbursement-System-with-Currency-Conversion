package com.i2i.user_management.Mapper;

import com.i2i.user_management.Dto.ExpenseResponseDto;
import com.i2i.user_management.Model.Expense;

/**
 * The ExpenseMapper class provides static utility methods for converting
 * between Expense entity objects and their corresponding ExpenseResponseDto
 * Data Transfer Objects (DTOs).
 *
 * @author Sabarinathan
 */
public class ExpenseMapper {


    /**
     * Converts an Expense entity into an ExpenseResponseDto.
     * This method maps all relevant fields, including ID, title, description,
     * expense date, amount, currency, receipt URL, and approval information.
     *
     * @param expense the Expense entity object to convert
     * @return an ExpenseResponseDto representing the given Expense,
     *         or null if the input is null
     */
    public static ExpenseResponseDto toDto(Expense expense) {
        if (expense == null) return null;
        ExpenseResponseDto dto = new ExpenseResponseDto();
        dto.setId(expense.getId());
        dto.setTitle(expense.getTitle());
        dto.setDescription(expense.getDescription());
        dto.setExpenseDate(expense.getExpenseDate());
        dto.setAmount(expense.getAmount());
        dto.setCurrency(expense.getCurrency());
        dto.setReceiptUrl(expense.getReceiptUrl());
        dto.setStatus(expense.getStatus() != null ? expense.getStatus().name() : null);
        dto.setRequestedBy(expense.getRequestedBy().getFirstName() + expense.getRequestedBy().getLastName());
        if (expense.getApprovedBy() != null) {
            dto.setApprovedByName(expense.getApprovedBy().getFirstName() + expense.getApprovedBy().getLastName());
        }
        dto.setRejectionReason(expense.getRejectionReason());
        return dto;
    }
}
