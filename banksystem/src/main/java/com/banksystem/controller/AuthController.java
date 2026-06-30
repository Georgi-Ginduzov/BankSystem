package com.banksystem.controller;

import com.banksystem.dto.LoginRequestDTO;
import com.banksystem.dto.LoginResponseDTO;
import com.banksystem.dto.RegisterRequestDTO;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.model.Employee;
import com.banksystem.security.JwtUtil;
import com.banksystem.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmployeeService employeeService;
    private final JwtUtil jwtUtil;

    public AuthController(EmployeeService employeeService, JwtUtil jwtUtil) {
        this.employeeService = employeeService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            var employee = employeeService.authenticate(request);
            return ResponseEntity.ok(issueToken(employee));
        } catch (BusinessException e) {
            return ResponseEntity.status(401).body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            var employee = employeeService.register(request);
            return ResponseEntity.status(201).body(issueToken(employee));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(new ErrorResponseDTO(e.getMessage()));
        }
    }

    private LoginResponseDTO issueToken(Employee employee) {
        String token = jwtUtil.generateToken(employee.getEmail(), employee.getRole().name());
        return new LoginResponseDTO(token, employee.getEmail(), employee.getRole().name(), employee.getUcn());
    }
}