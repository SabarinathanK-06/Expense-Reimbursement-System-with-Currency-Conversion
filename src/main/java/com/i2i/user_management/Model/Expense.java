package com.i2i.user_management.Model;

import com.i2i.user_management.Enum.ExpenseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity representing an expense submitted by an employee.
 * Supports multi-currency expense entries with automatic INR conversion.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@Data
@SuperBuilder
@NoArgsConstructor
@Table(name = "expenses")
@Entity
public class Expense extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @NonNull
    @Column(nullable = false)
    private String title;

    private String description;

    @NonNull
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @NonNull
    @Column(nullable = false)
    private BigDecimal amount;

    @NonNull
    @Column(nullable = false, length = 3)
    private String currency;

    @NonNull
    @Column(name = "receipt_url")
    private String receiptUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User approvedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(nullable = false)
    private Boolean isDeleted = false;

}
