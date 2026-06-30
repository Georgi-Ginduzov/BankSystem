package com.banksystem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AnalyticsOverviewDto {
    private long totalAccounts;
    private long activeAccounts;
    private long pendingLoans;
    private long activeLoans;
    private long paidOffLoans;
    private BigDecimal outstandingLoanBalance;
    private BigDecimal totalLoanOriginations;
}
