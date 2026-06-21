package com.banksystem.service;

import com.banksystem.dto.LoginRequestDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Employee;
import com.banksystem.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private Employee admin;

    @BeforeEach
    void setUp() {
        admin = new Employee();
        admin.setId(1);
        admin.setUcn("1234567890");
        admin.setEmail("admin@bank.com");
        admin.setEncryptedPassword(encoder.encode("admin123"));
        admin.setRole(Employee.Role.ADMIN);
    }

    @Test
    void authenticate_success() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@bank.com");
        request.setPassword("admin123");

        when(employeeRepository.findByEmail("admin@bank.com")).thenReturn(Optional.of(admin));

        Employee result = employeeService.authenticate(request);
        assertThat(result).isEqualTo(admin);
    }

    @Test
    void authenticate_wrongPassword_throwsException() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@bank.com");
        request.setPassword("wrong");

        when(employeeRepository.findByEmail("admin@bank.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> employeeService.authenticate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void authenticate_emailNotFound_throwsException() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("unknown@bank.com");
        request.setPassword("admin123");

        when(employeeRepository.findByEmail("unknown@bank.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.authenticate(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void createDefaultAdmin_whenNotExists_createsAdmin() {
        when(employeeRepository.findByEmail("admin@bank.com")).thenReturn(Optional.empty());
        when(employeeRepository.save(any(Employee.class))).thenReturn(admin);

        employeeService.createDefaultAdmin();

        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void createDefaultAdmin_whenExists_doesNotCreateAgain() {
        when(employeeRepository.findByEmail("admin@bank.com")).thenReturn(Optional.of(admin));

        employeeService.createDefaultAdmin();

        verify(employeeRepository, never()).save(any());
    }
}