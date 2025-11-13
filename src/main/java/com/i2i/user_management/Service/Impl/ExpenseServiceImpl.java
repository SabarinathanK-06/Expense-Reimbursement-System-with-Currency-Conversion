package com.i2i.user_management.Service.Impl;

import com.i2i.user_management.Dto.CurrencySummaryDto;
import com.i2i.user_management.Dto.EmployeeExpenseSummaryDto;
import com.i2i.user_management.Dto.ExpenseApprovalDto;
import com.i2i.user_management.Dto.ExpenseRequestDto;
import com.i2i.user_management.Dto.ExpenseResponseDto;
import com.i2i.user_management.Enum.ExpenseStatus;
import com.i2i.user_management.Exception.ApplicationException;
import com.i2i.user_management.Exception.AuthenticationFailedException;
import com.i2i.user_management.Exception.BadRequestException;
import com.i2i.user_management.Exception.ConflictException;
import com.i2i.user_management.Exception.DatabaseException;
import com.i2i.user_management.Exception.ExternalServiceException;
import com.i2i.user_management.Exception.NotFoundException;
import com.i2i.user_management.Integration.Client.ExchangeRateClient;
import com.i2i.user_management.Mapper.ExpenseMapper;
import com.i2i.user_management.Model.Expense;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.ExpenseRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.ExpenseService;
import com.i2i.user_management.util.ValidationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing employee expenses and admin approval workflow.
 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseServiceImpl.class);

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExchangeRateClient exchangeRateClient;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              UserRepository userRepository,
                              ExchangeRateClient exchangeRateClient) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.exchangeRateClient = exchangeRateClient;
    }

    /**
     * Submits a new expense for the currently logged-in employee.
     * Performs currency conversion and persists both original and INR values.
     *
     * @param request        DTO containing expense details
     * @param submitterEmail email of the logged-in user submitting the expense
     * @return persisted expense details
     * @throws NotFoundException if user not found
     * @throws BadRequestException if input validation fails
     * @throws ExternalServiceException if currency conversion API fails
     */
    @Override
    @Transactional
    public ExpenseResponseDto submitExpense(ExpenseRequestDto request, String submitterEmail) {
        try {
            User user = userRepository.findByEmail(ValidationUtils.requestedNonNull(submitterEmail))
                    .orElseThrow(() -> new NotFoundException("User not found: " + submitterEmail));

            if (request.getAmount().signum() <= 0) {
                throw new BadRequestException("Expense amount must be greater than zero");
            }
            if (request.getExpenseDate() == null) {
                throw new BadRequestException("Expense date is required");
            }

            validateCurrencyCode(request.getCurrency(), null);
            Expense expense = Expense.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .expenseDate(request.getExpenseDate())
                    .amount(request.getAmount())
                    .currency(request.getCurrency().toUpperCase())
                    .receiptUrl(request.getReceiptUrl())
                    .status(ExpenseStatus.PENDING)
                    .requestedBy(user)
                    .isDeleted(false)
                    .build();

            Expense saved = expenseRepository.save(expense);
            log.info("Expense submitted successfully by {} for {} in {}",
                    user.getEmail(), request.getAmount(), request.getCurrency());

            ExpenseResponseDto expenseResponseDto = ExpenseMapper.toDto(saved);
            try {
                BigDecimal rate = exchangeRateClient.getRateToInr(expense.getCurrency());
                expenseResponseDto.setAmountInInr(expense.getAmount().multiply(rate));
            } catch (Exception e) {
                log.warn("Failed to convert currency for expense ID: {} | Currency: {} | Requested By: {} | Reason: {}",
                        saved.getId(), saved.getCurrency(), saved.getRequestedBy().getEmail(), e.getMessage());
                throw new ExternalServiceException("Unable to fetch exchange rate for "
                        + expense.getCurrency(), e);
            }
            return expenseResponseDto;

        } catch (BadRequestException | NotFoundException | ExternalServiceException e) {
            log.warn("Expense submission failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during expense submission: {}", e.getMessage(), e);
            throw new RuntimeException("Internal server error while submitting expense");
        }
    }

    /**
     * Retrieves all expenses submitted by the logged-in user.
     *
     * @param userEmail current user's email
     * @param pageable  pagination info
     * @return page of expenses belonging to the user
     * @throws NotFoundException if user not found
     */
    @Override
    public Page<ExpenseResponseDto> getExpensesForCurrentUser(String userEmail, Pageable pageable) {
        User user = userRepository.findByEmail(ValidationUtils.requestedNonNull(userEmail))
                .orElseThrow(() -> new NotFoundException("User not found: " + userEmail));

        Page<Expense> page = expenseRepository.findAllByRequestedByIdAndIsDeletedFalse(user.getId(), pageable);
        log.debug("Fetched {} expenses for user {}", page.getTotalElements(), user.getEmail());
        return page.map(expense -> {
            ExpenseResponseDto dto = ExpenseMapper.toDto(expense);
            try {
                BigDecimal rate = exchangeRateClient.getRateToInr(expense.getCurrency());
                dto.setAmountInInr(expense.getAmount().multiply(rate));

                log.trace("Converted {} {} to {} INR for expense {} at rate {}",
                        expense.getAmount(), expense.getCurrency(), dto.getAmountInInr(),
                        expense.getId(), rate);
            } catch (Exception e) {
                log.warn("Failed to fetch exchange rate for currency {} (expense {}): {}",
                        expense.getCurrency(), expense.getId(), e.getMessage());
                throw new ExternalServiceException("Unable to fetch exchange rate for "
                        + expense.getCurrency(), e);
            }
            return dto;
        });
    }

    /**
     * Updates an existing expense record when the expense status is still in PENDING.
     *
     * @param expenseId the unique identifier of the expense to update
     * @param request   the ExpenseUpdateDto containing updated expense details
     * @param userEmail the email of the currently authenticated user performing the update
     * @return a ExpenseResponseDto containing the updated expense details including recalculated INR amount
     * @throws NotFoundException              if the expense is not found in the system
     * @throws BadRequestException            if mandatory fields are missing or invalid
     * @throws ConflictException              if the expense is not in a modifiable (PENDING) state
     * @throws AuthenticationFailedException  if a non-owner user attempts to update the expense
     * @throws ExternalServiceException       if the currency conversion API fails or returns invalid data
     */
    @Override
    public ExpenseResponseDto updateExpense(UUID expenseId, ExpenseRequestDto request, String userEmail) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NotFoundException("Expense not found with id: " + expenseId));

        validateUpdateRequest(expense, request, userEmail);
        validateCurrencyCode(request.getCurrency(), expenseId);

        expense.setTitle(request.getTitle());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency().toUpperCase());
        expense.setReceiptUrl(request.getReceiptUrl());
        Expense saved = expenseRepository.save(expense);
        log.info("Expense {} updated successfully by user {}", expenseId, userEmail);

        ExpenseResponseDto expenseResponseDto = ExpenseMapper.toDto(saved);

        BigDecimal rate;
        try {
            rate = exchangeRateClient.getRateToInr(request.getCurrency());
            expenseResponseDto.setAmountInInr(expense.getAmount().multiply(rate));
            log.trace("Converted {} {} to {} INR (rate={}) for expense {}",
                    expense.getAmount(), expense.getCurrency(), expenseResponseDto.getAmountInInr(), rate, expense.getId());
        } catch (Exception ex) {
            log.warn("Exchange rate lookup failed for currency {}: {}", request.getCurrency(), ex.getMessage());
            throw new ExternalServiceException("Unable to fetch exchange rate for " + request.getCurrency(), ex);
        }
        return expenseResponseDto;
    }

    /**
     * Validates whether an expense update request is allowed based on business rules and data integrity checks.
     *
     * @param expense   the existing Expense entity fetched from the database
     * @param request   the ExpenseUpdateDto containing the new expense details
     * @param userEmail the email of the currently authenticated user attempting the update
     *
     * @throws BadRequestException           if the expense is deleted, amount is invalid, or expense date is missing
     * @throws ConflictException             if the expense is not in ExpenseStatus#PENDING state
     * @throws AuthenticationFailedException if the user attempting to update is not the owner of the expense
     */
    private void validateUpdateRequest(Expense expense, ExpenseRequestDto request, String userEmail) {
        if (Boolean.TRUE.equals(expense.getIsDeleted())) {
            throw new BadRequestException("Expense already deleted");
        }
        if (expense.getStatus() != ExpenseStatus.PENDING) {
            throw new ConflictException("Only pending expenses can be updated");
        }
        if (!expense.getRequestedBy().getEmail().equalsIgnoreCase(userEmail)) {
            throw new AuthenticationFailedException("You are not allowed to update this expense");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new BadRequestException("Expense amount must be greater than zero");
        }
        if (request.getExpenseDate() == null) {
            throw new BadRequestException("Expense date is required");
        }
    }

    /**
     * Validates whether the provided currency code exists in the available currency list
     * fetched from the external exchange rate API.
     *
     * @param currencyCode the currency code provided in the request
     * @param expenseId    the expense ID being updated (used for logging context)
     * @throws BadRequestException if the given currency code is not part of the supported list
     */
    private void validateCurrencyCode(String currencyCode, UUID expenseId) {
        if (currencyCode == null || currencyCode.isBlank()) {
            log.warn("Empty or null currency provided{}",
                    expenseId != null ? " for expense " + expenseId : "");
            throw new BadRequestException("Currency code must not be empty or null");
        }

        Map<String, String> availableCurrencies = getAllCurrencies();
        String upperCurrency = currencyCode.toUpperCase();
        if (!availableCurrencies.containsKey(upperCurrency)) {
            log.warn("Invalid currency '{}' provided{}",
                    upperCurrency, expenseId != null ? " for expense " + expenseId : "");
            throw new BadRequestException(
                    "Invalid currency code: " + upperCurrency +
                            ". Please use one of: " + String.join(", ", availableCurrencies.keySet())
            );
        }
        log.debug("Validated currency '{}'{}", upperCurrency,
                expenseId != null ? " for expense " + expenseId : "");
    }



    /**
     * Retrieves all available currencies with their corresponding codes and names
     * from the external exchange rate API via the ExchangeRateClient.
     *
     * @return a map containing currency codes as keys and currency names as values
     */
    @Override
    public Map<String, String> getAllCurrencies() {
        log.debug("Fetching all available currencies and their codes from the external exchange rate API");
        try {
            Map<String, String> currencies = exchangeRateClient.getAllCurrencies();
            if (currencies == null || currencies.isEmpty()) {
                log.warn("Received empty or null response from the exchange rate API");
                throw new ExternalServiceException("No currencies returned from the exchange rate service");
            }
            log.info("Successfully retrieved {} currencies from the external API", currencies.size());
            return currencies;

        } catch (ExternalServiceException e) {
            log.warn("Exchange rate service error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch currencies from external API: {}", e.getMessage(), e);
            throw new ExternalServiceException("Unable to fetch available currencies from external service", e);
        }
    }



    /**
     * Soft deletes an Expense (marks as deleted) based on their ID and only deletes the pending statuses expense.
     * This does not permanently remove the record.
     *
     * @param id ID of expense to approve
     */
    @Override
    @Transactional
    public void deleteExpense(UUID id) {
        if (id == null) {
            throw new BadRequestException("Expense ID cannot be null");
        }

        log.info("Request received to delete expense with ID: {}", id);
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense not found for id: " + id));
        if (Boolean.TRUE.equals(expense.getIsDeleted())) {
            log.warn("Expense with ID {} is already deleted", id);
            throw new BadRequestException("Expense already deleted for id: " + id);
        }

        if (expense.getStatus() != ExpenseStatus.PENDING) {
            log.warn("Expense with ID {} cannot be deleted. Current status: {}", id, expense.getStatus());
            throw new BadRequestException("Only expenses in 'PENDING' status can be deleted. But current status: "+ expense.getStatus());
        }
        expenseRepository.softDelete(id);
        log.info("Expense with ID {} marked as deleted successfully", id);
    }


    /**
     * Fetches all expenses for admin review based on optional filters:
     * status, from-date, to-date.
     *
     * @param status expense status filter
     * @param from   optional start date
     * @param to     optional end date
     * @param pageable pagination information
     * @return filtered expense list for admin
     */
    @Override
    public Page<ExpenseResponseDto> getExpensesForAdmin(String status, LocalDate from, LocalDate to, Pageable pageable) {
        ExpenseStatus filterStatus = null;
        try {
            if (status != null && !status.isBlank()) {
                filterStatus = ExpenseStatus.valueOf(status.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid expense status: " + status);
        }

        Page<Expense> page = expenseRepository.findAllByFilters(filterStatus, from, to, pageable);
        log.debug("Admin fetched {} expenses with filters status={} from={} to={}",
                page.getTotalElements(), status, from, to);

        return page.map(expense -> {
            ExpenseResponseDto dto = ExpenseMapper.toDto(expense);
            try {
                BigDecimal rate = exchangeRateClient.getRateToInr(expense.getCurrency());
                dto.setAmountInInr(expense.getAmount().multiply(rate));
                log.trace("Converted {} {} to {} INR (rate={}) for expense {}",
                        expense.getAmount(), expense.getCurrency(), dto.getAmountInInr(), rate, expense.getId());
            } catch (Exception e) {
                log.warn("Failed to convert currency {} for expense {}: {}",
                        expense.getCurrency(), expense.getId(), e.getMessage());
                throw new ExternalServiceException("Unable to fetch exchange rate for "
                        + expense.getCurrency(), e);
            }
            return dto;
        });
    }

    /**
     * Retrieves a specific expense by ID.
     *
     * @param id expense ID
     * @param requesterEmail email of requester
     * @return expense details
     */
    @Override
    public ExpenseResponseDto getExpenseById(UUID id, String requesterEmail) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Expense not found: " + id));
        ExpenseResponseDto expenseResponseDto = ExpenseMapper.toDto(expense);
        try {
            BigDecimal rate = exchangeRateClient.getRateToInr(expense.getCurrency());
            expenseResponseDto.setAmountInInr(expense.getAmount().multiply(rate));
        } catch (Exception e) {
            log.warn("Currency conversion failed for expense {} (currency: {}) requested by {}. Reason: {}",
                    id, expense.getCurrency(), expense.getRequestedBy(), e.getMessage());
            throw new ExternalServiceException("Unable to fetch exchange rate for "
                    + expense.getCurrency(), e);
        }
        log.debug("Expense {} retrieved by {}", id, requesterEmail);
        return expenseResponseDto;
    }

    /**
     * Approves or rejects a pending expense.
     * Only Finance Admins are expected to call this method.
     *
     * @param expenseId ID of expense to approve
     * @param approverEmail email of approver
     * @param dto ExpenseApprovalDto
     * @throws ConflictException if expense is not pending
     * @throws NotFoundException if expense or approver not found
     */
    @Override
    @Transactional
    public void approveOrRejectExpense(UUID expenseId, String approverEmail, ExpenseApprovalDto dto) {
        try {
            Expense expense = expenseRepository.findById(expenseId)
                    .orElseThrow(() -> new NotFoundException("Expense not found: " + expenseId));

            if (expense.getStatus() != ExpenseStatus.PENDING) {
                throw new ConflictException("Expense already " + expense.getStatus());
            }

            User approver = userRepository.findByEmail(ValidationUtils.requestedNonNull(approverEmail))
                    .orElseThrow(() -> new NotFoundException("Approver not found: " + approverEmail));

            //always check and implement business
            if (approver.getEmail().equalsIgnoreCase(expense.getRequestedBy().getEmail())) {
                log.warn("Approver {} attempted to approve their own expense {}", approverEmail, expenseId);
                throw new ConflictException("You cannot approve or reject your own expense request.");
            }

            if (ExpenseStatus.APPROVED.name().equalsIgnoreCase(dto.getStatus())) {
                log.info("Approving expense ID: {} by approver: {}", expenseId, approverEmail);
                expense.setStatus(ExpenseStatus.APPROVED);
            } else if (ExpenseStatus.REJECTED.name().equalsIgnoreCase(dto.getStatus())) {
                log.info("Rejecting expense ID: {} by approver: {} with reason: {}", expenseId, approverEmail, dto.getReason());
                expense.setStatus(ExpenseStatus.REJECTED);
                expense.setRejectionReason(dto.getReason());
            } else {
                log.warn("Invalid status '{}' received for expense ID: {}", dto.getStatus(), expenseId);
                throw new ConflictException("Invalid status value: " + dto.getStatus() + "Only can be ");
            }

            expense.setApprovedBy(approver);
            expense.setUpdatedAt(LocalDateTime.now());
            expenseRepository.save(expense);

            log.info("Expense {} approved or rejected by {}", expenseId, approverEmail);
        } catch (NotFoundException | ConflictException e) {
            log.warn("Approval failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error approving expense {}: {}", expenseId, e.getMessage(), e);
            throw new ApplicationException("Error approving expense");
        }
    }

    /**
     * Generates a report of total approved expenses per employee.
     *
     * @param from start date
     * @param to end date
     * @return list of employee expense summaries
     */
    @Override
    public List<EmployeeExpenseSummaryDto> reportTotalApprovedPerEmployee(LocalDate from, LocalDate to) {
        List<EmployeeExpenseSummaryDto> summaries;

        try {
            summaries = expenseRepository.totalApprovedPerEmployee(from, to);
        } catch (Exception e) {
            log.warn("Failed to fetch total approved expenses per employee between {} and {}: {}", from, to, e.getMessage());
            throw new DatabaseException("Error retrieving approved expense summaries from database", e);
        }

        return summaries.stream()
                .map(summary -> {
                    try {
                        BigDecimal rate = exchangeRateClient.getRateToInr(summary.getCurrency());
                        summary.setTotalApprovedInInr(summary.getTotalAmount().multiply(rate));

                        log.trace("Converted {} {} for employee {} to {} INR (rate {})",
                                summary.getTotalAmount(),
                                summary.getCurrency(),
                                summary.getEmployeeName(),
                                summary.getTotalApprovedInInr(),
                                rate);
                    } catch (Exception e) {
                        log.warn("Failed to convert currency {} for employee {}: {}",
                                summary.getCurrency(), summary.getEmployeeName(), e.getMessage());
                        throw new ExternalServiceException("Unable to fetch exchange rate for "
                                + summary.getCurrency(), e);
                    }
                    return summary;
                })
                .collect(Collectors.toList());
    }


    /**
     * Generates a report summarizing expenses by currency and total INR value.
     *
     * @param from start date (optional)
     * @param to end date (optional)
     * @return list of currency-wise totals
     */
    @Override
    public List<CurrencySummaryDto> reportTotalByCurrency(String currency, LocalDate from, LocalDate to) {
        validateCurrencyCode(currency, null);
        List<CurrencySummaryDto> summaries;

        try {
            summaries = expenseRepository.totalByCurrency(currency.toUpperCase(), from, to);
        } catch (Exception e) {
            log.warn("Failed to fetch total by currency between {} and {}: {}", from, to, e.getMessage());
            throw new DatabaseException("Error retrieving approved expense summaries from database", e);
        }
        return summaries.stream()
                .map(summary -> {
                    try {
                        BigDecimal rate = exchangeRateClient.getRateToInr(summary.getCurrency());
                        summary.setTotalAmountInInr(summary.getTotalOriginalAmount().multiply(rate));
                        log.trace("Converted {} {} to {} INR at rate {}",
                                summary.getTotalOriginalAmount(), summary.getCurrency(),
                                summary.getTotalAmountInInr(), rate);
                    } catch (Exception e) {
                        log.warn("Failed to convert currency {}: {}", summary.getCurrency(), e.getMessage());
                        throw new ExternalServiceException("Unable to fetch exchange rate for "
                                + summary.getCurrency(), e);
                    }
                    return summary;
                })
                .collect(Collectors.toList());
    }
}
