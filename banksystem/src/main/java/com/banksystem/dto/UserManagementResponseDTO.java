package com.banksystem.dto;

import com.banksystem.model.Employee;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserManagementResponseDTO {
    private Integer id;
    private String ucn;
    private String email;
    private Employee.Role role;
    private String firstName;
    private String lastName;
}
