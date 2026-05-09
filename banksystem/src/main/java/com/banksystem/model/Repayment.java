package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Repayment
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @NotNull(message = "Month number is required")
    @Column(name = "month_number", nullable = false)
    private Integer monthNumber;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentStatus status = RepaymentStatus.PENDING;

    public enum RepaymentStatus
    {
        PENDING, PAID, OVERDUE
    }

    @Column(name = "actual_payment_amount", precision = 19, scale = 2)
    private BigDecimal actualPaymentAmount;

    @Column(name = "actual_interest_amount", precision = 19, scale = 2)
    private BigDecimal actualInterestAmount;

    @Column(name = "actual_principal_amount", precision = 19, scale = 2)
    private BigDecimal actualPrincipalAmount;

    @Column(name = "actual_remaining_to_pay", precision = 19, scale = 2)
    private BigDecimal actualRemainingToPay;

    @NotNull(message = "Expected payment amount is required")
    @Column(name = "expected_payment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedPaymentAmount;

    @NotNull(message = "Expected interest amount is required")
    @Column(name = "expected_interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedInterestAmount;

    @NotNull(message = "Expected principal amount is required")
    @Column(name = "expected_principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedPrincipalAmount;

    @NotNull(message = "Expected remaining to pay is required")
    @Column(name = "expected_remaining_to_pay", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedRemainingToPay;

    @PreUpdate
    protected void onUpdate()
    {
        if (status == RepaymentStatus.PAID && paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }
}