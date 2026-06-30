package com.banksystem.dto;

import com.banksystem.model.Repayment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RepaymentPlanItemDTO {
    private Integer monthNumber;
    private LocalDate dueDate;
    private Repayment.RepaymentStatus status;
    private BigDecimal expectedPaymentAmount;
    private BigDecimal expectedPrincipalAmount;
    private BigDecimal expectedInterestAmount;
    private BigDecimal expectedRemainingToPay;
    private LocalDateTime paymentDate;
    private BigDecimal actualPaymentAmount;
}
