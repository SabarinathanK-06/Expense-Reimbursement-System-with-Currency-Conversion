package com.i2i.user_management.Controller;

import com.i2i.user_management.Dto.CurrencySummaryDto;
import com.i2i.user_management.Dto.EmployeeExpenseSummaryDto;
import com.i2i.user_management.Dto.ExpenseApprovalDto;
import com.i2i.user_management.Dto.ExpenseRequestDto;
import com.i2i.user_management.Dto.ExpenseResponseDto;
import com.i2i.user_management.Helper.SecurityContextHelper;
import com.i2i.user_management.Service.ExpenseService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller that handles expense-related operations.
 */
@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private static final Logger log = LoggerFactory.getLogger(ExpenseController.class);
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Endpoint for employees to submit a new expense claim.
     *
     * @param request DTO containing expense details (title, amount, date, currency, etc.)
     * @return Created expense with INR conversion details
     */
    @PostMapping("/create")
    public ResponseEntity<ExpenseResponseDto> submitExpense(@Valid @RequestBody ExpenseRequestDto request) {
        String email = SecurityContextHelper.extractEmailFromContext();
        log.info("User {} is submitting a new expense", email);
        ExpenseResponseDto response = expenseService.submitExpense(request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint for employees to view all their submitted expenses.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return Page of the user's expenses with current status
     */
    @GetMapping
    public ResponseEntity<Page<ExpenseResponseDto>> getMyExpenses(Pageable pageable) {
        String email = SecurityContextHelper.extractEmailFromContext();
        log.debug("Fetching expenses for user {}", email);
        Page<ExpenseResponseDto> page = expenseService.getExpensesForCurrentUser(email, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Updates an existing expense record if it is in the PENDING state.
     * Only the user who originally submitted the expense can update it.
     * Validates currency, amount, and date before applying changes.
     *
     * @param id  the UUID of the expense to update
     * @param expenseRequestDto the updated expense details
     * @return 200 OK with the updated ExpenseResponseDto
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponseDto> updateExpense(@PathVariable("id") UUID id,
                                                            @Valid @RequestBody ExpenseRequestDto expenseRequestDto) {
        String email =  SecurityContextHelper.extractEmailFromContext();
        log.info("Updating requested expenses for user {}", email);
        ExpenseResponseDto expenseResponseDto = expenseService.updateExpense(id, expenseRequestDto, email);
        return ResponseEntity.ok(expenseResponseDto);
    }

    /**
     * Retrieves a list of all supported currencies along with their corresponding currency codes.
     *
     * @return ResponseEntity containing a map of currency codes and their names with HTTP 200 OK status
     */
    @GetMapping("/currencies")
    public ResponseEntity<Map<String, String>> getCurrencies() {
        log.info("Fetching all available currencies and their codes");
        return ResponseEntity.ok(expenseService.getAllCurrencies());
    }


    /**
     * Deletes a pending expense by its ID. Once approved or rejected,
     * they cannot be modified or removed.
     *
     * @param id the unique identifier of the expense to delete
     * @return 204 No Content if deletion is successful
     */
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteExpense(@PathVariable("id") UUID id) {
        String email = SecurityContextHelper.extractEmailFromContext();
        log.info("User {} requested to delete expense with ID {}", email, id);

        expenseService.deleteExpense(id);
        log.info("Expense with ID {} deleted successfully by user {}", id, email);
        return ResponseEntity.noContent().build();
    }


    /**
     * Admin endpoint to fetch all expenses with optional filters:
     * status (Pending/Approved/Rejected), date range, and pagination.
     *
     * @param status optional filter for expense status
     * @param from optional start date
     * @param to optional end date
     * @param pageable pagination settings
     * @return Filtered and paginated list of expenses
     */
    @GetMapping("/all")
    public ResponseEntity<Page<ExpenseResponseDto>> getAllExpensesForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {

        log.debug("Admin fetching expenses with filters: status={}, from={}, to={}", status, from, to);
        Page<ExpenseResponseDto> page = expenseService.getExpensesForAdmin(status, from, to, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Admin endpoint to approve a pending expense.
     *
     * @param id expense ID to approve
     * @param dto ExpenseApprovalDto
     * @return 204 No Content on success
     */
    @PostMapping("/{id}/action")
    public ResponseEntity<Void> approveOrRejectExpense(@PathVariable("id") UUID id,
                                                       @Valid @RequestBody ExpenseApprovalDto dto) {
        String email = SecurityContextHelper.extractEmailFromContext();
        log.info("Admin {} approving or rejecting the expense {}", email, id);
        expenseService.approveOrRejectExpense(id, email, dto);
        return ResponseEntity.noContent().build();
    }

    /**
     * Admin endpoint to generate report:
     * total approved expense amount per employee within an optional date range.
     *
     * @param from optional start date filter
     * @param to optional end date filter
     * @return List of employee-wise total approved expenses in INR
     */
    @GetMapping("/report/approved-per-employee")
    public ResponseEntity<List<EmployeeExpenseSummaryDto>> reportPerEmployee(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("Admin generating employee-wise report from {} to {}", from, to);
        List<EmployeeExpenseSummaryDto> result = expenseService.reportTotalApprovedPerEmployee(from, to);
        return ResponseEntity.ok(result);
    }

    /**
     * Admin endpoint to generate report:
     * total approved expenses grouped by currency and converted INR values.
     *
     * @param from optional start date
     * @param to optional end date
     * @return List of currency-based totals
     */
    @GetMapping("/report/by-currency")
    public ResponseEntity<List<CurrencySummaryDto>> reportByCurrency(
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        log.debug("Admin generating currency-based report from {} to {}", from, to);
        List<CurrencySummaryDto> result = expenseService.reportTotalByCurrency( currency, from, to);
        return ResponseEntity.ok(result);
    }


}
