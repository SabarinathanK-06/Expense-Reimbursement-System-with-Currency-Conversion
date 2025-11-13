package com.i2i.user_management.service.impl;

import com.i2i.user_management.Dto.CurrencySummaryDto;
import com.i2i.user_management.Dto.EmployeeExpenseSummaryDto;
import com.i2i.user_management.Dto.ExpenseApprovalDto;
import com.i2i.user_management.Dto.ExpenseRequestDto;
import com.i2i.user_management.Dto.ExpenseResponseDto;
import com.i2i.user_management.Enum.ExpenseStatus;
import com.i2i.user_management.Exception.AuthenticationFailedException;
import com.i2i.user_management.Exception.BadRequestException;
import com.i2i.user_management.Exception.ConflictException;
import com.i2i.user_management.Exception.DatabaseException;
import com.i2i.user_management.Exception.ExternalServiceException;
import com.i2i.user_management.Exception.NotFoundException;
import com.i2i.user_management.Integration.Client.ExchangeRateClient;
import com.i2i.user_management.Model.Expense;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.ExpenseRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.Impl.ExpenseServiceImpl;
import com.i2i.user_management.util.TestConstants;
import com.i2i.user_management.util.TestData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private User user;

    private Expense expense;

    @BeforeEach
    void setUp() {
        user = TestData.getUser();
        expense = TestData.getExpense(user);
    }

    @Test
    void submitExpense_Success() {
        //arrange
        ExpenseRequestDto requestDto = TestData.getExpenseRequestDto();
        when(userRepository.findByEmail(TestConstants.EMAIL)).thenReturn(Optional.of(user));
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD)).thenReturn(TestConstants.RATE);

        //act
        ExpenseResponseDto response = expenseService.submitExpense(requestDto, user.getEmail());

        //assert
        assertNotNull(response);
        assertEquals(ExpenseStatus.PENDING, expense.getStatus());
        verify(expenseRepository, times(1)).save(any(Expense.class));
        verify(exchangeRateClient, times(1)).getRateToInr(requestDto.getCurrency());
    }

    @Test
    void submitExpense_ShouldThrow_NotFound_WhenUserNotFound() {
        //arrange
        ExpenseRequestDto request = TestData.getExpenseRequestDto();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(NotFoundException.class,
                () -> expenseService.submitExpense(request, "unknown@mail.com"));
    }

    @Test
    void submitExpense_ShouldThrow_ExternalServiceException_WhenGetAllCurrenciesFails() {
        //arrange
        ExpenseRequestDto request = TestData.getExpenseRequestDto();
        when(userRepository.findByEmail(TestConstants.EMAIL)).thenReturn(Optional.of(user));
        when(exchangeRateClient.getAllCurrencies()).thenThrow(new ExternalServiceException("Rate API down"));

        //act & assert
        assertThrows(ExternalServiceException.class,
                () -> expenseService.submitExpense(request, user.getEmail()));
    }

    @Test
    void submitExpense_ShouldThrow_ExternalServiceException_WhenRateFails() {
        //arrange
        ExpenseRequestDto request = TestData.getExpenseRequestDto();
        when(userRepository.findByEmail(TestConstants.EMAIL)).thenReturn(Optional.of(user));
        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());
        when(expenseRepository.save(any())).thenReturn(expense);
        when(exchangeRateClient.getRateToInr(anyString())).thenThrow(new RuntimeException("Rate API down"));

        //act & assert
        assertThrows(ExternalServiceException.class,
                () -> expenseService.submitExpense(request, user.getEmail()));
    }

    @Test
    void getExpensesForCurrentUser_Success() {
        //arrange
        Pageable pageable = PageRequest.of(0, 5);
        Page<Expense> page = new PageImpl<>(List.of(expense));
        when(userRepository.findByEmail(TestConstants.EMAIL)).thenReturn(Optional.of(user));
        when(expenseRepository.findAllByRequestedByIdAndIsDeletedFalse(eq(user.getId()), eq(pageable)))
                .thenReturn(page);
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD)).thenReturn(TestConstants.RATE);

        //act
        Page<ExpenseResponseDto> result = expenseService.getExpensesForCurrentUser(user.getEmail(), pageable);

        //assert
        assertEquals(1, result.getTotalElements());
        verify(exchangeRateClient, times(1)).getRateToInr(expense.getCurrency());
    }

    @Test
    void getExpensesForCurrentUser_ShouldThrow_WhenExchangeRateFails() {
        //arrange
        Pageable pageable = PageRequest.of(0, 5);
        Page<Expense> page = new PageImpl<>(List.of(expense));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(expenseRepository.findAllByRequestedByIdAndIsDeletedFalse(eq(user.getId()), eq(pageable)))
                .thenReturn(page);
        when(exchangeRateClient.getRateToInr(anyString())).thenThrow(new RuntimeException("API down"));

        //act & assert
        assertThrows(ExternalServiceException.class,
                () -> expenseService.getExpensesForCurrentUser(user.getEmail(), pageable));
    }

    @Test
    void updateExpense_Success() {
        //arrange
        ExpenseRequestDto updateDto = TestData.getExpenseRequestDto();
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));
        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD)).thenReturn(TestConstants.RATE);

        //act
        ExpenseResponseDto response = expenseService.updateExpense(expense.getId(), updateDto, user.getEmail());

        //assert
        assertNotNull(response);
        assertEquals(response.getExpenseDate(), expense.getExpenseDate());
        verify(expenseRepository, times(1)).save(any(Expense.class));
    }

    @Test
    void updateExpense_ShouldThrow_WhenNotOwner() {
        //arrange
        ExpenseRequestDto updateDto = TestData.getExpenseRequestDto();
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act & assert
        assertThrows(AuthenticationFailedException.class,
                () -> expenseService.updateExpense(expense.getId(), updateDto, "another@mail.com"));
    }

    @Test
    void updateExpense_ShouldThrow_WhenDeletedExpenseUpdated() {
        //arrange
        ExpenseRequestDto updateDto = TestData.getExpenseRequestDto();
        expense.setIsDeleted(true);
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act & assert
        assertThrows(BadRequestException.class,
                () -> expenseService.updateExpense(expense.getId(), updateDto, "another@mail.com"));
    }

    @Test
    void updateExpense_ShouldThrow_WhenApprovedExpenseUpdated() {
        //arrange
        ExpenseRequestDto updateDto = TestData.getExpenseRequestDto();
        expense.setStatus(ExpenseStatus.APPROVED);
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act & assert
        assertThrows(ConflictException.class,
                () -> expenseService.updateExpense(expense.getId(), updateDto, "another@mail.com"));
    }

    @Test
    void deleteExpense_Success() {
        //arrange
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act
        expenseService.deleteExpense(expense.getId());

        //assert
        verify(expenseRepository, times(1)).softDelete(expense.getId());
    }

    @Test
    void deleteExpense_ShouldThrow_WhenAlreadyDeleted() {
        //arrange
        expense.setIsDeleted(true);
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act & assert
        assertThrows(BadRequestException.class, () -> expenseService.deleteExpense(expense.getId()));
    }

    @Test
    void deleteExpense_ShouldThrow_WhenNotFound() {
        //arrange
        when(expenseRepository.findById(any())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(NotFoundException.class, () -> expenseService.deleteExpense(expense.getId()));
    }

    @Test
    void deleteExpense_ShouldThrow_WhenStatusIsApproved() {
        //arrange
        expense.setStatus(ExpenseStatus.APPROVED);
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act & assert
        assertThrows(BadRequestException.class, () -> expenseService.deleteExpense(expense.getId()));
    }

    @Test
    void getAllCurrencies_Success() {
        //arrange
        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());

        //act
        Map<String, String> result = expenseService.getAllCurrencies();

        //assert
        assertEquals(4, result.size());
    }

    @Test
    void getAllCurrencies_ShouldThrow_WhenEmpty() {
        //arrange
        when(exchangeRateClient.getAllCurrencies()).thenReturn(Collections.emptyMap());

        //act & assert
        assertThrows(ExternalServiceException.class, () -> expenseService.getAllCurrencies());
    }

    @Test
    void getAllCurrencies_ShouldThrow_WhenNull() {
        //arrange
        when(exchangeRateClient.getAllCurrencies()).thenReturn(null);

        //act & assert
        assertThrows(ExternalServiceException.class, () -> expenseService.getAllCurrencies());
    }

    @Test
    void approveExpense_Success() {
        //arrange
        ExpenseApprovalDto dto = TestData.createApprovalDto(String.valueOf(ExpenseStatus.APPROVED));
        when(expenseRepository.findById(any())).thenReturn(Optional.ofNullable(expense));
        User userToApprove = User.builder()
                .id(UUID.randomUUID())
                .email("finance@mail.com")
                .firstName("paari")
                .lastName("seerangan")
                .password("password123")
                .isActive(true)
                .isDeleted(false)
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userToApprove));

        //act
        expenseService.approveOrRejectExpense(expense.getId(), "finance@mail.com", dto);

        //assert
        verify(expenseRepository, times(1)).save(any());
        assertEquals(ExpenseStatus.APPROVED, expense.getStatus());
    }

    @Test
    void approveExpense_WhenApproverAndRequesterAreSame() {
        //arrange
        ExpenseApprovalDto dto = TestData.createApprovalDto(String.valueOf(ExpenseStatus.APPROVED));
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));
        when(userRepository.findByEmail("finance@mail.com")).thenReturn(Optional.of(user));

        //act & assert
        assertThrows(ConflictException.class,
                () -> expenseService.approveOrRejectExpense(expense.getId(), "finance@mail.com", dto));
    }

    @Test
    void approveExpense_WhenExpenseStatusIsApproved() {
        //arrange
        ExpenseApprovalDto dto = TestData.createApprovalDto(String.valueOf(ExpenseStatus.APPROVED));
        expense.setStatus(ExpenseStatus.APPROVED);
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));

        //act & assert
        assertThrows(ConflictException.class,
                () -> expenseService.approveOrRejectExpense(expense.getId(), "finance@mail.com", dto));
    }

    @Test
    void approveExpense_WhenUSerIsNull() {
        //arrange
        ExpenseApprovalDto dto = TestData.createApprovalDto(String.valueOf(ExpenseStatus.APPROVED));
        when(expenseRepository.findById(any())).thenReturn(Optional.of(expense));
        when(userRepository.findByEmail("finance@mail.com")).thenReturn(Optional.empty());

        //act & assert
        assertThrows(NotFoundException.class,
                () -> expenseService.approveOrRejectExpense(expense.getId(), "finance@mail.com", dto));
    }

    @Test
    void rejectExpense_Success() {
        //arrange
        ExpenseApprovalDto dto = TestData.createApprovalDto(String.valueOf(ExpenseStatus.REJECTED));
        when(expenseRepository.findById(any())).thenReturn(Optional.ofNullable(expense));
        User userToApprove = User.builder()
                .id(UUID.randomUUID())
                .email("finance@mail.com")
                .firstName("paari")
                .lastName("seerangan")
                .password("password123")
                .isActive(true)
                .isDeleted(false)
                .build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(userToApprove));

        //act
        expenseService.approveOrRejectExpense(expense.getId(), "finance@mail.com", dto);

        //assert
        verify(expenseRepository, times(1)).save(any());
        assertEquals(ExpenseStatus.REJECTED, expense.getStatus());
    }

    @Test
    void reportTotalApprovedPerEmployee_Success() {
        //arrange
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        when(expenseRepository.totalApprovedPerEmployee(from, to)).thenReturn(TestData.getEmployeeExpenseSummaries());
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD)).thenReturn(TestConstants.RATE);
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_EUR)).thenReturn(TestConstants.RATE);

        //act
        List<EmployeeExpenseSummaryDto> result = expenseService.reportTotalApprovedPerEmployee(from, to);

        //assert
        assertNotNull(result);
        assertEquals(2, result.size());

        EmployeeExpenseSummaryDto test1 = result.get(0);
        assertEquals(BigDecimal.valueOf(20000), test1.getTotalApprovedInInr());

        EmployeeExpenseSummaryDto test2 = result.get(1);
        assertEquals(BigDecimal.valueOf(40000), test2.getTotalApprovedInInr());

        verify(expenseRepository, times(1)).totalApprovedPerEmployee(from, to);
        verify(exchangeRateClient, times(1)).getRateToInr(TestConstants.CURRENCY_USD);
        verify(exchangeRateClient, times(1)).getRateToInr(TestConstants.CURRENCY_EUR);
    }


    @Test
    void reportTotalApprovedPerEmployee_ShouldThrow_WhenExchangeRateFails() {
        //arrange
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(expenseRepository.totalApprovedPerEmployee(from, to)).thenReturn(TestData.getEmployeeExpenseSummaries());
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD))
                .thenThrow(new RuntimeException("Rate API down"));

        //act & assert
        assertThrows(ExternalServiceException.class,
                () -> expenseService.reportTotalApprovedPerEmployee(from, to));

        verify(expenseRepository, times(1)).totalApprovedPerEmployee(from, to);
        verify(exchangeRateClient, times(1)).getRateToInr(TestConstants.CURRENCY_USD);
    }

    @Test
    void reportTotalApprovedPerEmployee_ShouldThrow_WhenDatabaseException() {
        //arrange
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(expenseRepository.totalApprovedPerEmployee(from, to))
                .thenThrow(new DatabaseException("Failed to return data"));

        //act & assert
        assertThrows(DatabaseException.class,
                () -> expenseService.reportTotalApprovedPerEmployee(from, to));
    }

    @Test
    void reportTotalByCurrency_Success() {
        //arrange
        String currency = TestConstants.CURRENCY_USD;
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        List<CurrencySummaryDto> summaries = TestData.getCurrencySummaries();

        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());
        when(expenseRepository.totalByCurrency(currency, from, to)).thenReturn(summaries);
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD)).thenReturn(BigDecimal.valueOf(80));
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_EUR)).thenReturn(BigDecimal.valueOf(90));

        //act
        List<CurrencySummaryDto> result = expenseService.reportTotalByCurrency(currency, from, to);

        //assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(BigDecimal.valueOf(16000), result.get(0).getTotalAmountInInr());
        assertEquals(BigDecimal.valueOf(18000), result.get(1).getTotalAmountInInr());

        verify(expenseRepository, times(1))
                .totalByCurrency(currency.toUpperCase(), from, to);
    }

    @Test
    void reportTotalByCurrency_ShouldThrow_WhenCurrencyInvalid() {
        //arrange
        String invalidCurrency = "XYZ";
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());

        //act & assert
        assertThrows(BadRequestException.class,
                () -> expenseService.reportTotalByCurrency(invalidCurrency, from, to));

        verify(exchangeRateClient, times(1)).getAllCurrencies();
    }

    @Test
    void reportTotalByCurrency_ShouldThrow_WhenDatabaseException() {
        //arrange
        String currency = TestConstants.CURRENCY_USD;
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);

        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());
        when(expenseRepository.totalByCurrency(currency, from, to))
                .thenThrow(new RuntimeException("Failed to fetch data"));

        //act & assert
        assertThrows(DatabaseException.class,
                () -> expenseService.reportTotalByCurrency(currency, from, to));
    }

    @Test
    void reportTotalByCurrency_ShouldThrow_WhenGetRateToInrFails() {
        //arrange
        String currency = TestConstants.CURRENCY_USD;
        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = LocalDate.of(2025, 12, 31);
        List<CurrencySummaryDto> summaries = TestData.getCurrencySummaries();

        when(exchangeRateClient.getAllCurrencies()).thenReturn(TestData.getAllCurrenciesApiResponse());
        when(expenseRepository.totalByCurrency(currency, from, to)).thenReturn(summaries);
        when(exchangeRateClient.getRateToInr(TestConstants.CURRENCY_USD))
                .thenThrow(new RuntimeException("Failed to get rate"));

        //act & assert
        assertThrows(ExternalServiceException.class,
                () -> expenseService.reportTotalByCurrency(currency, from, to));
        verify(expenseRepository, times(1)).totalByCurrency(currency, from, to);
    }

}
