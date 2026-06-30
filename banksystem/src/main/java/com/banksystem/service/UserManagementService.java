package com.banksystem.service;

import com.banksystem.dto.UserManagementRequestDTO;
import com.banksystem.dto.UserManagementResponseDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.exception.ResourceNotFoundException;
import com.banksystem.model.Client;
import com.banksystem.model.Customer;
import com.banksystem.model.Employee;
import com.banksystem.repository.ClientRepository;
import com.banksystem.repository.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserManagementService {

    private final EmployeeRepository employeeRepository;
    private final ClientRepository clientRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserManagementService(EmployeeRepository employeeRepository, ClientRepository clientRepository) {
        this.employeeRepository = employeeRepository;
        this.clientRepository = clientRepository;
    }

    public List<UserManagementResponseDTO> searchUsers(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

        return employeeRepository.findAllByOrderByIdAsc()
                .stream()
                .filter(employee -> matchesQuery(employee, normalizedQuery))
                .map(this::mapUser)
                .toList();
    }

    @Transactional
    public UserManagementResponseDTO createUser(UserManagementRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedUcn = normalizeUcn(request.getUcn());
        Employee.Role role = requireRole(request.getRole());
        String password = requirePassword(request.getPassword());

        if (employeeRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException("An account with this email already exists");
        }

        if (employeeRepository.findByUcn(normalizedUcn).isPresent()) {
            throw new BusinessException("An account with this UCN already exists");
        }

        Employee employee = new Employee();
        employee.setEmail(normalizedEmail);
        employee.setUcn(normalizedUcn);
        employee.setRole(role);
        employee.setEncryptedPassword(passwordEncoder.encode(password));

        if (role == Employee.Role.CUSTOMER) {
            upsertCustomerProfile(normalizedUcn, request.getFirstName(), request.getLastName());
        }

        return mapUser(employeeRepository.save(employee));
    }

    @Transactional
    public UserManagementResponseDTO updateUser(Integer userId, UserManagementRequestDTO request) {
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String normalizedEmail = normalizeEmail(request.getEmail());
        Employee.Role targetRole = requireRole(request.getRole());

        if (!employee.getEmail().equalsIgnoreCase(normalizedEmail)
                && employeeRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BusinessException("An account with this email already exists");
        }

        String requestUcn = normalizeUcn(request.getUcn());
        if (!employee.getUcn().equals(requestUcn)) {
            if (employee.getRole() == Employee.Role.CUSTOMER || targetRole == Employee.Role.CUSTOMER) {
                throw new BusinessException("Changing UCN is not supported for customer users");
            }

            if (employeeRepository.findByUcn(requestUcn).isPresent()) {
                throw new BusinessException("An account with this UCN already exists");
            }

            employee.setUcn(requestUcn);
        }

        employee.setEmail(normalizedEmail);
        employee.setRole(targetRole);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (request.getPassword().trim().length() < 6) {
                throw new BusinessException("Password must be at least 6 characters");
            }
            employee.setEncryptedPassword(passwordEncoder.encode(request.getPassword().trim()));
        }

        if (targetRole == Employee.Role.CUSTOMER) {
            upsertCustomerProfile(employee.getUcn(), request.getFirstName(), request.getLastName());
        }

        return mapUser(employeeRepository.save(employee));
    }

    @Transactional
    public void deleteUser(Integer userId, String currentUserEmail) {
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (employee.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new BusinessException("You cannot delete the account you are currently using");
        }

        employeeRepository.delete(employee);
    }

    private boolean matchesQuery(Employee employee, String query) {
        if (query.isEmpty()) {
            return true;
        }

        String role = employee.getRole().name().toLowerCase(Locale.ROOT);
        if (employee.getEmail().toLowerCase(Locale.ROOT).contains(query)
                || employee.getUcn().toLowerCase(Locale.ROOT).contains(query)
                || role.contains(query)) {
            return true;
        }

        UserManagementResponseDTO mapped = mapUser(employee);
        return (mapped.getFirstName() != null && mapped.getFirstName().toLowerCase(Locale.ROOT).contains(query))
                || (mapped.getLastName() != null && mapped.getLastName().toLowerCase(Locale.ROOT).contains(query));
    }

    private UserManagementResponseDTO mapUser(Employee employee) {
        String firstName = null;
        String lastName = null;

        Optional<Client> client = clientRepository.findById(employee.getUcn());
        if (client.isPresent() && client.get() instanceof Customer customer) {
            firstName = customer.getFirstName();
            lastName = customer.getLastName();
        }

        return new UserManagementResponseDTO(
                employee.getId(),
                employee.getUcn(),
                employee.getEmail(),
                employee.getRole(),
                firstName,
                lastName
        );
    }

    private void upsertCustomerProfile(String ucn, String firstName, String lastName) {
        String normalizedFirstName = firstName == null ? "" : firstName.trim();
        String normalizedLastName = lastName == null ? "" : lastName.trim();

        if (normalizedFirstName.isEmpty() || normalizedLastName.isEmpty()) {
            throw new BusinessException("Customer users require first name and last name");
        }

        Optional<Client> existingClient = clientRepository.findById(ucn);
        if (existingClient.isPresent()) {
            if (existingClient.get() instanceof Customer customer) {
                customer.setFirstName(normalizedFirstName);
                customer.setLastName(normalizedLastName);
                clientRepository.save(customer);
                return;
            }

            throw new BusinessException("A non-customer client already exists with this identifier");
        }

        clientRepository.save(new Customer(ucn, normalizedFirstName, normalizedLastName));
    }

    private String normalizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUcn(String ucn) {
        if (ucn == null || !ucn.trim().matches("\\d{10}")) {
            throw new BusinessException("UCN must contain exactly 10 digits");
        }
        return ucn.trim();
    }

    private Employee.Role requireRole(Employee.Role role) {
        if (role == null) {
            throw new BusinessException("Role is required");
        }
        return role;
    }

    private String requirePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException("Password is required");
        }

        if (password.trim().length() < 6) {
            throw new BusinessException("Password must be at least 6 characters");
        }

        return password.trim();
    }
}
