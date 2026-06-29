package com.banksystem.service;

import com.banksystem.dto.ClientFrontendDTO;
import com.banksystem.dto.ClientRequestDTO;
import com.banksystem.dto.LoanSummaryDto;
import com.banksystem.exception.BusinessException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.model.Loan;
import com.banksystem.model.Merchant;
import com.banksystem.repository.ClientRepository;
import com.banksystem.repository.LoanRepository;
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

    public List<LoanSummaryDto> getLoansByClientId(String id) {
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
                        .loanTypeName(loan.getLoanType() != null ? loan.getLoanType().getName() : "Unknown")
                        .termMonths(loan.getTermMonths())
                        .monthlyPayment(loan.getMonthlyPayment())
                        .build())
                .toList();
    }

    private String generateClientId(String email) {
        return email.trim().toLowerCase();
    }

    private String generateUcn(String email) {
        String hash = String.valueOf(Math.abs(email.hashCode()));
        if (hash.length() >= 10) {
            return hash.substring(0, 10);
        } else {
            return String.format("%10s", hash).replace(' ', '0');
        }
    }

    @Transactional
    public Client addClientFromFrontend(ClientFrontendDTO request) {
        String clientId = request.getId() != null && !request.getId().isEmpty()
                ? request.getId()
                : generateClientId(request.getEmail());

        if (clientRepository.existsById(clientId)) {
            throw new BusinessException("Client with this ID already exists. Please use update or a different identifier.");
        }

        // Parse fullName into first and last, with fallbacks
        String fullName = request.getFullName() != null ? request.getFullName().trim() : "";
        String[] nameParts = fullName.split(" ", 2);
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";
        if (firstName.isEmpty()) firstName = "Unknown";
        if (lastName.isEmpty()) lastName = "Unknown";

        String ucn = generateUcn(request.getEmail());

        Customer customer = new Customer(ucn, firstName, lastName);
        customer.setId(clientId);
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setSegment(request.getSegment() != null ? request.getSegment() : "RETAIL");

        return clientRepository.save(customer);
    }

    @Transactional
    public Client updateClientFromFrontend(String id, ClientFrontendDTO request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (client instanceof Customer) {
            Customer c = (Customer) client;
            String fullName = request.getFullName() != null ? request.getFullName().trim() : "";
            String[] nameParts = fullName.split(" ", 2);
            String firstName = nameParts.length > 0 ? nameParts[0] : "";
            String lastName = nameParts.length > 1 ? nameParts[1] : "";
            if (firstName.isEmpty()) firstName = "Unknown";
            if (lastName.isEmpty()) lastName = "Unknown";

            c.setFirstName(firstName);
            c.setLastName(lastName);
            c.setEmail(request.getEmail());
            c.setPhone(request.getPhone());
            c.setAddress(request.getAddress());
            c.setSegment(request.getSegment());
            return clientRepository.save(c);
        } else if (client instanceof Merchant) {
            Merchant m = (Merchant) client;
            // For merchant, we treat fullName as company name
            m.setCompanyName(request.getFullName() != null ? request.getFullName() : "Unknown Company");
            m.setEmail(request.getEmail());
            m.setPhone(request.getPhone());
            m.setAddress(request.getAddress());
            m.setSegment(request.getSegment());
            return clientRepository.save(m);
        }
        return client;
    }

    public ClientFrontendDTO getClientFrontendById(String id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        ClientFrontendDTO dto = new ClientFrontendDTO();
        dto.setId(client.getId());
        dto.setEmail(client.getEmail());
        dto.setPhone(client.getPhone());
        dto.setAddress(client.getAddress());
        dto.setSegment(client.getSegment());

        if (client instanceof Customer) {
            Customer c = (Customer) client;
            dto.setFullName(c.getFirstName() + " " + c.getLastName());
        } else if (client instanceof Merchant) {
            Merchant m = (Merchant) client;
            dto.setFullName(m.getCompanyName());
        }
        return dto;
    }

}