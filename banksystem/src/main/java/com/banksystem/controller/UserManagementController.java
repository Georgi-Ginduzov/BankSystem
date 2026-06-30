package com.banksystem.controller;

import com.banksystem.dto.UserManagementRequestDTO;
import com.banksystem.dto.UserManagementResponseDTO;
import com.banksystem.service.UserManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private final UserManagementService userManagementService;

    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public ResponseEntity<List<UserManagementResponseDTO>> searchUsers(
            @RequestParam(defaultValue = "") String query
    ) {
        return ResponseEntity.ok(userManagementService.searchUsers(query));
    }

    @PostMapping
    public ResponseEntity<UserManagementResponseDTO> createUser(@RequestBody UserManagementRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userManagementService.createUser(request));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserManagementResponseDTO> updateUser(
            @PathVariable Integer userId,
            @RequestBody UserManagementRequestDTO request
    ) {
        return ResponseEntity.ok(userManagementService.updateUser(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer userId, Authentication authentication) {
        userManagementService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
