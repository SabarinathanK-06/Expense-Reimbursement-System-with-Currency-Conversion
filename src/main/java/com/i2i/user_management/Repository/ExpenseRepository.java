package com.i2i.user_management.Repository;

import com.i2i.user_management.Dto.CurrencySummaryDto;
import com.i2i.user_management.Dto.EmployeeExpenseSummaryDto;
import com.i2i.user_management.Enum.ExpenseStatus;
import com.i2i.user_management.Model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Page<Expense> findAllByRequestedByIdAndIsDeletedFalse(UUID userId, Pageable pageable);

    Page<Expense> findAllByStatusAndIsDeletedFalse(ExpenseStatus status, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.isDeleted = false "
            + "AND e.id = :id AND (:status IS NULL OR e.status = :status)")
    Optional<Expense> findByIdAndStatus(@Param("id") UUID id, @Param("status") ExpenseStatus status);

    @Modifying
    @Query("UPDATE Expense e SET e.isDeleted = true WHERE e.id = :id")
    void softDelete(@Param("id") UUID id);

    @Query("SELECT e FROM Expense e WHERE e.isDeleted = false "
            + "AND (:status IS NULL OR e.status = :status) "
            + "AND (e.expenseDate >= COALESCE(:fromDate, e.expenseDate)) "
            + "AND (e.expenseDate <= COALESCE(:toDate, e.expenseDate))")
    Page<Expense> findAllByFilters(@Param("status") ExpenseStatus status,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate,
                                   Pageable pageable);

    @Query("SELECT new com.i2i.user_management.Dto.EmployeeExpenseSummaryDto("
            + "e.requestedBy.employeeId, "
            + "CONCAT(e.requestedBy.firstName, ' ', e.requestedBy.lastName), "
            + "e.currency, "
            + "SUM(e.amount)) "
            + "FROM Expense e "
            + "WHERE e.isDeleted = false AND e.status = 'APPROVED' "
            + "AND (e.expenseDate >= COALESCE(:fromDate, e.expenseDate)) "
            + "AND (e.expenseDate <= COALESCE(:toDate, e.expenseDate)) "
            + "GROUP BY e.requestedBy.employeeId, e.requestedBy.firstName, e.requestedBy.lastName, e.currency")
    List<EmployeeExpenseSummaryDto> totalApprovedPerEmployee(@Param("fromDate") LocalDate fromDate,
                                                             @Param("toDate") LocalDate toDate);


    @Query("SELECT new com.i2i.user_management.Dto.CurrencySummaryDto(e.currency, SUM(e.amount)) "
            + "FROM Expense e "
            + "WHERE e.isDeleted = false AND e.status = 'APPROVED' "
            + "AND (:currency IS NULL OR e.currency = :currency) "
            + "AND (e.expenseDate >= COALESCE(:fromDate, e.expenseDate)) "
            + "AND (e.expenseDate <= COALESCE(:toDate, e.expenseDate)) "
            + "GROUP BY e.currency")
    List<CurrencySummaryDto> totalByCurrency(@Param("currency") String currency,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Query("SELECT new com.i2i.user_management.Dto.CurrencySummaryDto(e.currency, SUM(e.amount)) "
            + "FROM Expense e "
            + "WHERE e.isDeleted = false AND e.status = 'APPROVED' "
            + "AND (e.expenseDate >= COALESCE(:fromDate, e.expenseDate)) "
            + "AND (e.expenseDate <= COALESCE(:toDate, e.expenseDate)) "
            + "GROUP BY e.currency")
    List<CurrencySummaryDto> groupByCurrency(@Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);


}
