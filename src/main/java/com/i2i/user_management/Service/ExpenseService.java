package com.i2i.user_management.Service;

import com.i2i.user_management.Dto.CurrencySummaryDto;
import com.i2i.user_management.Dto.EmployeeExpenseSummaryDto;
import com.i2i.user_management.Dto.ExpenseApprovalDto;
import com.i2i.user_management.Dto.ExpenseRequestDto;
import com.i2i.user_management.Dto.ExpenseResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ExpenseService {

    ExpenseResponseDto submitExpense(ExpenseRequestDto request, String submitterEmail);

    Page<ExpenseResponseDto> getExpensesForCurrentUser(String userEmail, Pageable pageable);

    ExpenseResponseDto updateExpense(UUID id, ExpenseRequestDto request, String submitterEmail);

    void deleteExpense(UUID id);

    Page<ExpenseResponseDto> getExpensesForAdmin(String status, LocalDate from, LocalDate to, Pageable pageable);

    ExpenseResponseDto getExpenseById(UUID id, String requesterEmail);

    void approveOrRejectExpense(UUID expenseId, String approverEmail, ExpenseApprovalDto dto);

    List<EmployeeExpenseSummaryDto> reportTotalApprovedPerEmployee(LocalDate from, LocalDate to);

    List<CurrencySummaryDto> reportTotalByCurrency(String currency, LocalDate from, LocalDate to);

    Map<String, String> getAllCurrencies();
}
