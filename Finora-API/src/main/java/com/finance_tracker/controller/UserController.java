package com.finance_tracker.controller;

import com.finance_tracker.dto.ApiResponse;
import com.finance_tracker.dto.UserResponseDTO;
import com.finance_tracker.model.Role;
import com.finance_tracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser() {
        // JWT subject is now the user's numeric ID (as a string)
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        Long userId = Long.parseLong(subject);
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUserId(userId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateRole(
            @PathVariable Long id,
            @RequestParam Role role) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateRole(id, role)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponseDTO>> setActiveStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success(userService.setActiveStatus(id, active)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }
}
