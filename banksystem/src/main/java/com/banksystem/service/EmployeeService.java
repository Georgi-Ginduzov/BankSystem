package com.banksystem.service;

import com.banksystem.dto.LoginRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Employee;
import com.banksystem.repository.EmployeeRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public Employee authenticate(LoginRequestDTO loginRequest) {
        Employee employee = employeeRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), employee.getEncryptedPassword())) {
            throw new BusinessException("Invalid email or password");
        }
        return employee;
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