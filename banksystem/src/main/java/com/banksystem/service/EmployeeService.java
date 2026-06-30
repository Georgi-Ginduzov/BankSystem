package com.banksystem.service;

import com.banksystem.dto.LoginRequestDTO;
import com.banksystem.dto.RegisterRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.model.Employee;
import com.banksystem.model.Merchant;
import com.banksystem.repository.ClientRepository;
import com.banksystem.repository.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public EmployeeService(EmployeeRepository employeeRepository, ClientRepository clientRepository) {
        this.employeeRepository = employeeRepository;
        this.clientRepository = clientRepository;
    }

    public Employee authenticate(LoginRequestDTO loginRequest) {
        String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();

        Employee employee = employeeRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), employee.getEncryptedPassword())) {
            throw new BusinessException("Invalid email or password");
        }
        return employee;
    }

    @Transactional
    public Employee register(RegisterRequestDTO registerRequest) {
        String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
        String clientType = normalizeClientType(registerRequest.getClientType());
        String normalizedIdentifier = "MERCHANT".equals(clientType)
                ? normalizeEik(registerRequest.getEik())
                : normalizeUcn(registerRequest.getUcn());

        if (employeeRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException("An account with this email already exists");
        }

        if (employeeRepository.findByUcn(normalizedIdentifier).isPresent()) {
            throw new BusinessException("An account with this identifier already exists");
        }

        Employee employee = new Employee();
        employee.setUcn(normalizedIdentifier);
        employee.setEmail(normalizedEmail);
        employee.setEncryptedPassword(passwordEncoder.encode(registerRequest.getPassword()));
        employee.setRole(Employee.Role.CUSTOMER);

        if ("MERCHANT".equals(clientType)) {
            createMerchantProfile(registerRequest, normalizedIdentifier);
        } else {
            createCustomerProfile(registerRequest, normalizedIdentifier);
        }

        return employeeRepository.save(employee);
    }

    private void createCustomerProfile(RegisterRequestDTO registerRequest, String ucn) {
        String firstName = registerRequest.getFirstName() == null ? "" : registerRequest.getFirstName().trim();
        String lastName = registerRequest.getLastName() == null ? "" : registerRequest.getLastName().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            throw new BusinessException("Customer registration requires first name and last name");
        }

        Optional<Client> existingClient = clientRepository.findById(ucn);
        if (existingClient.isPresent()) {
            return;
        }

        Customer customer = new Customer(ucn, firstName, lastName);
        clientRepository.save(customer);
    }

    private void createMerchantProfile(RegisterRequestDTO registerRequest, String eik) {
        String companyName = registerRequest.getCompanyName() == null ? "" : registerRequest.getCompanyName().trim();
        String representativeFirstName = registerRequest.getRepresentativeFirstName() == null
                ? ""
                : registerRequest.getRepresentativeFirstName().trim();
        String representativeLastName = registerRequest.getRepresentativeLastName() == null
                ? ""
                : registerRequest.getRepresentativeLastName().trim();

        if (companyName.isEmpty() || representativeFirstName.isEmpty() || representativeLastName.isEmpty()) {
            throw new BusinessException("Merchant registration requires company and representative names");
        }

        Optional<Client> existingClient = clientRepository.findById(eik);
        if (existingClient.isPresent()) {
            return;
        }

        Merchant merchant = new Merchant(eik, companyName, representativeFirstName, representativeLastName);
        clientRepository.save(merchant);
    }

    private String normalizeClientType(String clientType) {
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new BusinessException("Client type is required");
        }

        String normalized = clientType.trim().toUpperCase(Locale.ROOT);
        if (!"CUSTOMER".equals(normalized) && !"MERCHANT".equals(normalized)) {
            throw new BusinessException("Client type must be CUSTOMER or MERCHANT");
        }

        return normalized;
    }

    private String normalizeUcn(String ucn) {
        if (ucn == null || !ucn.trim().matches("\\d{10}")) {
            throw new BusinessException("UCN must contain exactly 10 digits");
        }
        return ucn.trim();
    }

    private String normalizeEik(String eik) {
        if (eik == null || !eik.trim().matches("\\d{9}|\\d{13}")) {
            throw new BusinessException("EIK must contain 9 or 13 digits");
        }
        return eik.trim();
    }

    public void createDefaultAdmin() {
        Optional<Employee> existing = employeeRepository.findByEmail("admin@bank.com");
        if (existing.isEmpty()) {
            Employee admin = new Employee();
            admin.setUcn("1234567890");
            admin.setEmail("admin@bank.com");
            admin.setEncryptedPassword(passwordEncoder.encode("admin123"));
            admin.setRole(Employee.Role.ADMIN);
            employeeRepository.save(admin);
            System.out.println("Default admin created: admin@bank.com / admin123");
        }
    }
}
