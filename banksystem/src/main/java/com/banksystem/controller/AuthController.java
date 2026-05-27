package com.banksystem.controller;

import com.banksystem.dto.LoginRequestDTO;
import com.banksystem.dto.LoginResponseDTO;
import com.banksystem.dto.response.ErrorResponseDTO;
import com.banksystem.exception.BusinessException;
import com.banksystem.security.JwtUtil;
import com.banksystem.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
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
            String token = jwtUtil.generateToken(employee.getEmail(), employee.getRole().name());
            return ResponseEntity.ok(new LoginResponseDTO(token, employee.getEmail(), employee.getRole().name()));
        } catch (BusinessException e) {
            return ResponseEntity.status(401).body(new ErrorResponseDTO(e.getMessage()));
        }
    }
}