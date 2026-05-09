package com.banksystem.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "loan_type")
public class LoanType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Loan type name is required")
    @Column(unique = true, nullable = false)
    private String name;

    @NotNull(message = "Interest rate is required")
    @PositiveOrZero(message = "Interest rate must be positive or zero")
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @NotNull(message = "Maximum term is required")
    @Positive(message = "Maximum term must be positive")
    @Column(name = "max_term_months", nullable = false)
    private Integer maxTermMonths;

    @NotNull(message = "Maximum amount is required")
    @Positive(message = "Maximum amount must be positive")
    @Column(name = "max_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    public LoanType() {}

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getMaxTermMonths() { return maxTermMonths; }
    public void setMaxTermMonths(Integer maxTermMonths) { this.maxTermMonths = maxTermMonths; }

    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
}