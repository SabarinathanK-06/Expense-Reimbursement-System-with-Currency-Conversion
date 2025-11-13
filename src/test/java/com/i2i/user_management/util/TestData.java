package com.i2i.user_management.util;

import com.i2i.user_management.Dto.CurrencySummaryDto;
import com.i2i.user_management.Dto.EmployeeExpenseSummaryDto;
import com.i2i.user_management.Dto.ExpenseApprovalDto;
import com.i2i.user_management.Dto.ExpenseRequestDto;
import com.i2i.user_management.Dto.ExpenseResponseDto;
import com.i2i.user_management.Dto.RegisterDto;
import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Enum.ExpenseStatus;
import com.i2i.user_management.Model.Expense;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestData {

    public static User getUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("paari@mail.com")
                .firstName("paari")
                .lastName("seerangan")
                .password("password123")
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    public static Map<String, String> getAllCurrenciesApiResponse() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("USD", "United States Dollar");
        response.put("INR", "Indian Rupee");
        response.put("EUR", "Euro");
        response.put("GBP", "British Pound");
        return response;
    }


    public static Expense getExpense(User user) {
        return Expense.builder()
                .id(UUID.randomUUID())
                .title("Travel Expense")
                .description("Flight to client site")
                .expenseDate(LocalDate.now())
                .amount(BigDecimal.valueOf(200))
                .currency("USD")
                .status(ExpenseStatus.PENDING)
                .requestedBy(user)
                .isDeleted(false)
                .receiptUrl("http://example.com/receipt")
                .build();

    }

    public static ExpenseRequestDto getExpenseRequestDto() {
        ExpenseRequestDto dto = new ExpenseRequestDto();
        dto.setTitle("Hotel Bill");
        dto.setDescription("Stay during business trip");
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setCurrency("USD");
        dto.setExpenseDate(LocalDate.now());
        dto.setReceiptUrl("http://example.com/receipt");
        return dto;
    }

    public static ExpenseApprovalDto createApprovalDto(String status) {
        ExpenseApprovalDto dto = new ExpenseApprovalDto();
        dto.setStatus(status);
        dto.setReason(status.equalsIgnoreCase("REJECTED") ? "Invalid receipt" : null);
        return dto;
    }

    public static List<EmployeeExpenseSummaryDto> getEmployeeExpenseSummaries() {
        EmployeeExpenseSummaryDto s1 = new EmployeeExpenseSummaryDto();
        s1.setEmployeeName("Test name 1");
        s1.setCurrency("USD");
        s1.setTotalAmount(BigDecimal.valueOf(100));

        EmployeeExpenseSummaryDto s2 = new EmployeeExpenseSummaryDto();
        s2.setEmployeeName("Test name 2");
        s2.setCurrency("EUR");
        s2.setTotalAmount(BigDecimal.valueOf(200));

        return List.of(s1, s2);
    }

    public static List<CurrencySummaryDto> getCurrencySummaries() {
        CurrencySummaryDto s1 = new CurrencySummaryDto(TestConstants.CURRENCY_USD, TestConstants.RATE);

        CurrencySummaryDto s2 = new CurrencySummaryDto(TestConstants.CURRENCY_EUR, TestConstants.RATE);

        return List.of(s1, s2);
    }

    public static Role getRole(String roleName) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setName(roleName);
        role.setIsDeleted(false);
        return role;
    }



    public static UserDto getUserDto() {
        UserDto dto = new UserDto();
        dto.setFirstName("Paari");
        dto.setLastName("Seerangan");
        dto.setEmail("paari@mail.com");
        dto.setDepartment("IT");
        dto.setProject("Medtronic");
        dto.setRoleIds(List.of(UUID.randomUUID()));
        return dto;
    }

    public static RegisterDto getRegisterDto() {
        RegisterDto dto = new RegisterDto();
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail("john.doe@mail.com");
        dto.setPassword("password123");
        dto.setAddress("123 Street");
        dto.setDepartment("IT");
        dto.setProject("ProjectX");
        return dto;
    }


    public static ExpenseResponseDto createExpenseResponseDto() {
        ExpenseResponseDto dto = new ExpenseResponseDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Meal");
        dto.setCurrency("USD");
        dto.setAmount(BigDecimal.valueOf(50));
        dto.setAmountInInr(BigDecimal.valueOf(4000));
        return dto;
    }
}
