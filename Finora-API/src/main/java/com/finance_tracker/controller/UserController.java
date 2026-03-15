package com.finance_tracker.controller;

import com.finance_tracker.dto.*;
import com.finance_tracker.model.Role;
import com.finance_tracker.service.UserService;
import jakarta.validation.Valid;
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
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getUserByUserId(userId)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.updateProfile(userId, request)));
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

    @GetMapping("/vault/status")
    public ResponseEntity<ApiResponse<VaultStatusDTO>> getVaultStatus() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.getVaultStatus(userId)));
    }

    @PostMapping("/vault/enable")
    public ResponseEntity<ApiResponse<VaultStatusDTO>> enableVault(
            @Valid @RequestBody VaultEnableRequestDTO request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.enableVault(userId, request)));
    }

    @PostMapping("/vault/disable")
    public ResponseEntity<ApiResponse<VaultStatusDTO>> disableVault(
            @Valid @RequestBody VaultDisableRequestDTO request) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(userService.disableVault(userId, request)));
    }

    private Long getCurrentUserId() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        return Long.parseLong(subject);
    }
}
