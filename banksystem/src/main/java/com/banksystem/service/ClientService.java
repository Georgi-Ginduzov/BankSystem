package com.banksystem.service;

import com.banksystem.dto.ClientRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.model.Merchant;
import com.banksystem.repository.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

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
}