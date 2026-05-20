package com.banksystem.service;

import com.banksystem.dto.InstallmentPaymentDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.*;
import com.banksystem.repository.LoanRepository;
import com.banksystem.repository.RepaymentRepository;
import com.banksystem.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final RepaymentRepository repaymentRepository;
    private final AccountRepository accountRepository;

    public LoanService(LoanRepository loanRepository,
                       RepaymentRepository repaymentRepository,
                       AccountRepository accountRepository) {
        this.loanRepository = loanRepository;
        this.repaymentRepository = repaymentRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void markInstallmentAsPaid(InstallmentPaymentDTO request) {
        // Find the loan
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new BusinessException("Loan not found with id: " + request.getLoanId()));

        // Verify loan belongs to client
        if (!loan.getClient().getId().equals(request.getClientId())) {
            throw new BusinessException("Loan does not belong to the specified client");
        }

        // Verify loan is active
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BusinessException("Loan is not active. Current status: " + loan.getStatus());
        }

        // Find the repayment schedule for this month using Loan object
        Repayment repayment = repaymentRepository.findByLoanAndMonthNumber(loan, request.getMonthNumber())
                .orElseThrow(() -> new BusinessException("Repayment schedule not found for month " + request.getMonthNumber()));

        // Verify payment is not already paid
        if (repayment.getStatus() == Repayment.RepaymentStatus.PAID) {
            throw new BusinessException("Installment for month " + request.getMonthNumber() + " is already paid");
        }

        // Verify payment amount matches expected installment amount
        if (request.getPaymentAmount().compareTo(repayment.getExpectedPaymentAmount()) != 0) {
            throw new BusinessException(
                    String.format("Payment amount %.2f does not match expected installment amount %.2f",
                            request.getPaymentAmount(), repayment.getExpectedPaymentAmount())
            );
        }

        // Get the associated account
        Account account = loan.getAccount();

        // Verify account exists and is active
        if (account == null) {
            throw new BusinessException("Associated account not found");
        }

        // MEDIUM PRIORITY FIX: Check if account is already closed
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new BusinessException("Associated account is not active. Current status: " + account.getStatus());
        }

        // Verify sufficient funds
        if (account.getBalance().compareTo(request.getPaymentAmount()) < 0) {
            throw new BusinessException("Insufficient funds in account. Available: " + account.getBalance());
        }

        // Process payment: debit from account
        account.setBalance(account.getBalance().subtract(request.getPaymentAmount()));

        // Update repayment record
        repayment.setStatus(Repayment.RepaymentStatus.PAID);
        repayment.setPaymentDate(LocalDateTime.now());
        repayment.setActualPaymentAmount(request.getPaymentAmount());
        repayment.setActualInterestAmount(repayment.getExpectedInterestAmount());
        repayment.setActualPrincipalAmount(repayment.getExpectedPrincipalAmount());
        repayment.setActualRemainingToPay(repayment.getExpectedRemainingToPay());

        // Update loan remaining amount and paid installments count
        BigDecimal newRemainingAmount = loan.getRemainingAmount().subtract(repayment.getExpectedPrincipalAmount());
        loan.setRemainingAmount(newRemainingAmount);
        loan.setPaidInstallments(loan.getPaidInstallments() + 1);

        // Check if loan is fully paid
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                loan.getPaidInstallments() >= loan.getTermMonths()) {
            loan.setStatus(Loan.LoanStatus.PAID_OFF);
        }

        // Save all changes
        accountRepository.save(account);
        repaymentRepository.save(repayment);
        loanRepository.save(loan);
    }
}