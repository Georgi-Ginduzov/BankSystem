package com.banksystem.service;

import com.banksystem.dto.AnalyticsOverviewDto;
import com.banksystem.model.Account;
import com.banksystem.model.Loan;
import com.banksystem.repository.AccountRepository;
import com.banksystem.repository.LoanRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AnalyticsService {

    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;

    public AnalyticsService(AccountRepository accountRepository, LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.loanRepository = loanRepository;
    }

    public AnalyticsOverviewDto getOverview() {
        var accounts = accountRepository.findAll();
        var loans = loanRepository.findAll();

        long activeAccounts = accounts.stream()
                .filter(account -> account.getStatus() == Account.AccountStatus.ACTIVE)
                .count();

        long pendingLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.PENDING)
                .count();

        long activeLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.ACTIVE)
                .count();

        long paidOffLoans = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.PAID_OFF)
                .count();

        BigDecimal outstandingLoanBalance = loans.stream()
                .filter(loan -> loan.getStatus() == Loan.LoanStatus.ACTIVE)
                .map(Loan::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLoanOriginations = loans.stream()
                .map(Loan::getInitialAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new AnalyticsOverviewDto(
                accounts.size(),
                activeAccounts,
                pendingLoans,
                activeLoans,
                paidOffLoans,
                outstandingLoanBalance,
                totalLoanOriginations
        );
    }
}
