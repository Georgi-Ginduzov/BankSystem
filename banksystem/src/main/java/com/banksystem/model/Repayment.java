package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repayment")
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Loan ID is required")
    @Column(name = "loan_id", nullable = false)
    private Integer loanId;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "actual_payment_amount", precision = 19, scale = 2)
    private BigDecimal actualPaymentAmount;

    @NotNull(message = "Expected payment amount is required")
    @Column(name = "expected_payment_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedPaymentAmount;

    @Column(name = "actual_interest_amount", precision = 19, scale = 2)
    private BigDecimal actualInterestAmount;

    @NotNull(message = "Expected interest amount is required")
    @Column(name = "expected_interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedInterestAmount;

    @Column(name = "actual_principal_amount", precision = 19, scale = 2)
    private BigDecimal actualPrincipalAmount;

    @NotNull(message = "Expected principal amount is required")
    @Column(name = "expected_principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedPrincipalAmount;

    @NotNull(message = "Expected remaining to pay is required")
    @Column(name = "expected_remaining_to_pay", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedRemainingToPay;

    @Column(name = "actual_remaining_to_pay", precision = 19, scale = 2)
    private BigDecimal actualRemainingToPay;

    @NotNull(message = "Month number is required")
    @Column(name = "month_number", nullable = false)
    private Integer monthNumber;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RepaymentStatus status = RepaymentStatus.PENDING;

    public enum RepaymentStatus {
        PENDING, PAID, OVERDUE
    }

    @PreUpdate
    protected void onUpdate() {
        if (status == RepaymentStatus.PAID && paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }

    public Repayment() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getLoanId() { return loanId; }
    public void setLoanId(Integer loanId) { this.loanId = loanId; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public BigDecimal getActualPaymentAmount() { return actualPaymentAmount; }
    public void setActualPaymentAmount(BigDecimal actualPaymentAmount) { this.actualPaymentAmount = actualPaymentAmount; }

    public BigDecimal getExpectedPaymentAmount() { return expectedPaymentAmount; }
    public void setExpectedPaymentAmount(BigDecimal expectedPaymentAmount) { this.expectedPaymentAmount = expectedPaymentAmount; }

    public BigDecimal getActualInterestAmount() { return actualInterestAmount; }
    public void setActualInterestAmount(BigDecimal actualInterestAmount) { this.actualInterestAmount = actualInterestAmount; }

    public BigDecimal getExpectedInterestAmount() { return expectedInterestAmount; }
    public void setExpectedInterestAmount(BigDecimal expectedInterestAmount) { this.expectedInterestAmount = expectedInterestAmount; }

    public BigDecimal getActualPrincipalAmount() { return actualPrincipalAmount; }
    public void setActualPrincipalAmount(BigDecimal actualPrincipalAmount) { this.actualPrincipalAmount = actualPrincipalAmount; }

    public BigDecimal getExpectedPrincipalAmount() { return expectedPrincipalAmount; }
    public void setExpectedPrincipalAmount(BigDecimal expectedPrincipalAmount) { this.expectedPrincipalAmount = expectedPrincipalAmount; }

    public BigDecimal getExpectedRemainingToPay() { return expectedRemainingToPay; }
    public void setExpectedRemainingToPay(BigDecimal expectedRemainingToPay) { this.expectedRemainingToPay = expectedRemainingToPay; }

    public BigDecimal getActualRemainingToPay() { return actualRemainingToPay; }
    public void setActualRemainingToPay(BigDecimal actualRemainingToPay) { this.actualRemainingToPay = actualRemainingToPay; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public RepaymentStatus getStatus() { return status; }
    public void setStatus(RepaymentStatus status) { this.status = status; }
}