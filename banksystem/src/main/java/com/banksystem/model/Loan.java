package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan")
@NoArgsConstructor @AllArgsConstructor @Builder
public class Loan
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter @Setter

    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    @Getter @Setter
    private LoanType loanType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @Getter @Setter
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @Getter @Setter
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @Getter @Setter
    private Employee reviewedBy;

    @NotNull(message = "Initial amount is required")
    @Positive(message = "Initial amount must be positive")
    @Column(name = "initial_amount", nullable = false, precision = 19, scale = 2)
    @Getter @Setter
    private BigDecimal initialAmount;

    @NotNull(message = "Term in months is required")
    @Positive(message = "Term must be positive")
    @Column(name = "term_months", nullable = false)
    @Getter @Setter
    private Integer termMonths;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    @Getter @Setter
    private LocalDate startDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Getter @Setter
    private LoanStatus status = LoanStatus.PENDING;

    @NotNull(message = "Monthly payment is required")
    @Column(name = "monthly_payment", nullable = false, precision = 19, scale = 2)
    @Getter @Setter
    private BigDecimal monthlyPayment;

    @NotNull(message = "Remaining amount is required")
    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 2)
    @Getter @Setter
    private BigDecimal remainingAmount;

    @Column(name = "paid_installments", nullable = false)
    @Getter @Setter
    private Integer paidInstallments = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Getter
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @Getter
    private LocalDateTime updatedAt;

    public enum LoanStatus {
        PENDING,
        ACTIVE,
        PAID_OFF,
    }

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate()
    {
        updatedAt = LocalDateTime.now();
    }
}