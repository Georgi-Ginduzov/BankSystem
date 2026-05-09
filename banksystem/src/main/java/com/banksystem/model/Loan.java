package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull(message = "Loan type is required")
    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    @NotBlank(message = "Client ID is required")
    @Column(name = "client_id", nullable = false, length = 13)
    private String clientId;

    @NotNull(message = "Account ID is required")
    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @NotNull(message = "Initial amount is required")
    @Positive(message = "Initial amount must be positive")
    @Column(name = "initial_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal initialAmount;

    @NotNull(message = "Term in months is required")
    @Positive(message = "Term must be positive")
    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.PENDING;

    @NotNull(message = "Monthly payment is required")
    @Column(name = "monthly_payment", nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyPayment;

    @NotNull(message = "Remaining amount is required")
    @Column(name = "remaining_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "paid_installments", nullable = false)
    private Integer paidInstallments = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum LoanStatus {
        PENDING, ACTIVE, PAID_OFF, DEFAULTED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Loan() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getTypeId() { return typeId; }
    public void setTypeId(Integer typeId) { this.typeId = typeId; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public Integer getAccountId() { return accountId; }
    public void setAccountId(Integer accountId) { this.accountId = accountId; }

    public Integer getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Integer reviewedBy) { this.reviewedBy = reviewedBy; }

    public BigDecimal getInitialAmount() { return initialAmount; }
    public void setInitialAmount(BigDecimal initialAmount) { this.initialAmount = initialAmount; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }

    public BigDecimal getMonthlyPayment() { return monthlyPayment; }
    public void setMonthlyPayment(BigDecimal monthlyPayment) { this.monthlyPayment = monthlyPayment; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public Integer getPaidInstallments() { return paidInstallments; }
    public void setPaidInstallments(Integer paidInstallments) { this.paidInstallments = paidInstallments; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}