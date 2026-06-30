package com.banksystem.dto;

import com.banksystem.model.Employee;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserManagementRequestDTO {
    private String ucn;
    private String email;
    private String password;
    private Employee.Role role;
    private String firstName;
    private String lastName;
}
