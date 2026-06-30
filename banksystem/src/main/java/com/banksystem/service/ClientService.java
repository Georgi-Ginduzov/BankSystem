package com.banksystem.service;

import com.banksystem.dto.ClientRequestDTO;
import com.banksystem.dto.ClientLoanDetailsDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.dto.RepaymentPlanItemDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Account;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.model.Loan;
import com.banksystem.model.Merchant;
import com.banksystem.model.Repayment;
import com.banksystem.repository.AccountRepository;
import com.banksystem.repository.ClientRepository;
import com.banksystem.repository.LoanRepository;
import com.banksystem.repository.RepaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RepaymentRepository repaymentRepository;

    @Transactional
    public Client addClient(ClientRequestDTO request) {
        Client client;

        if ("CUSTOMER".equalsIgnoreCase(request.getClientType())) {
            // Validate required fields for customer
            if (request.getUcn() == null || request.getUcn().trim().isEmpty()) {
                throw new BusinessException("UCN is required for customer");
            }
            if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
                throw new BusinessException("First name is required for customer");
            }
            if (request.getLastName() == null || request.getLastName().trim().isEmpty()) {
                throw new BusinessException("Last name is required for customer");
            }

            // Check for duplicate UCN
            if (clientRepository.existsById(request.getUcn())) {
                throw new BusinessException("Customer with this UCN already exists");
            }

            client = new Customer(request.getUcn(), request.getFirstName(), request.getLastName());

        } else if ("MERCHANT".equalsIgnoreCase(request.getClientType())) {
            // Validate required fields for merchant
            if (request.getEik() == null || request.getEik().trim().isEmpty()) {
                throw new BusinessException("EIK is required for merchant");
            }
            if (request.getCompanyName() == null || request.getCompanyName().trim().isEmpty()) {
                throw new BusinessException("Company name is required for merchant");
            }
            if (request.getRepresentativeFirstName() == null || request.getRepresentativeFirstName().trim().isEmpty()) {
                throw new BusinessException("Representative first name is required for merchant");
            }
            if (request.getRepresentativeLastName() == null || request.getRepresentativeLastName().trim().isEmpty()) {
                throw new BusinessException("Representative last name is required for merchant");
            }

            // Check for duplicate EIK
            if (clientRepository.existsById(request.getEik())) {
                throw new BusinessException("Merchant with this EIK already exists");
            }

            client = new Merchant(request.getEik(), request.getCompanyName(),
                    request.getRepresentativeFirstName(), request.getRepresentativeLastName());
        } else {
            throw new BusinessException("Invalid client type. Must be 'CUSTOMER' or 'MERCHANT'");
        }

        return clientRepository.save(client);
    }

    public Client getClientById(String id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client not found with id: " + id));
    }

    public List<LoanSummaryDto> getLoansByClientId(String id)
    {
        return loanRepository
                .findByClient_Id(id)
                .stream()
                .map(loan -> LoanSummaryDto
                        .builder()
                        .id(loan.getId())
                        .initialAmount(loan.getInitialAmount())
                        .status(loan.getStatus())
                        .startDate(loan.getStartDate())
                        .remainingAmount(loan.getRemainingAmount())
                        .paidInstallments(loan.getPaidInstallments())
                        .loanTypeName(loan.getLoanType().getName())
                        .termMonths(loan.getTermMonths())
                        .monthlyPayment(loan.getMonthlyPayment())
                .build())
                .toList();
    }

    public List<Account> getAccountsByClientId(String id) {
        clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Client not found with id: " + id));

        return accountRepository.findByClientId(id);
    }

    public ClientLoanDetailsDTO getLoanDetailsByClientId(String clientId, Integer loanId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException("Client not found with id: " + clientId));

        Loan loan = loanRepository.findByIdAndClient_Id(loanId, clientId)
                .orElseThrow(() -> new BusinessException("Loan not found for the specified client"));

        List<RepaymentPlanItemDTO> repaymentPlan = repaymentRepository.findByLoanOrderByMonthNumberAsc(loan)
                .stream()
                .map(repayment -> new RepaymentPlanItemDTO(
                        repayment.getMonthNumber(),
                        repayment.getDueDate(),
                        repayment.getStatus(),
                        repayment.getExpectedPaymentAmount(),
                        repayment.getExpectedPrincipalAmount(),
                        repayment.getExpectedInterestAmount(),
                        repayment.getExpectedRemainingToPay(),
                        repayment.getPaymentDate(),
                        repayment.getActualPaymentAmount()
                ))
                .toList();

        return new ClientLoanDetailsDTO(
                loan.getId(),
                loan.getLoanType().getName(),
                loan.getStatus(),
                loan.getInitialAmount(),
                loan.getRemainingAmount(),
                loan.getMonthlyPayment(),
                loan.getTermMonths(),
                loan.getPaidInstallments(),
                loan.getStartDate(),
                loan.getSettlementAccount() != null ? loan.getSettlementAccount().getId() : null,
                loan.getSettlementAccount() != null ? loan.getSettlementAccount().getIban() : null,
                repaymentPlan
        );
    }
}
