package com.finance_tracker.service;

import com.finance_tracker.dto.*;
import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.exception.ValidationException;
import com.finance_tracker.model.Role;
import com.finance_tracker.model.User;
import com.finance_tracker.repository.UserRepository;
import com.finance_tracker.utils.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FieldEncryptionService encryptionService;

    private static final String VAULT_CONFIRM_TEXT = "I understand I will permanently lose all data if I lose this passphrase";

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        String username = request.getUsername().trim().toLowerCase();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ValidationException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException("Email already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setActive(true);

        User saved = userRepository.save(user);
        String token = jwtService.generateToken(saved.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .user(toDTO(saved))
                .build();
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!user.isActive()) {
            throw new ValidationException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .user(toDTO(user))
                .build();
    }

    @Transactional
    public UserResponseDTO updateProfile(Long userId, UpdateProfileRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (request.getUsername() != null) {
            String newUsername = request.getUsername().trim().toLowerCase();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername)) {
                    throw new ValidationException("Username already taken");
                }
                user.setUsername(newUsername);
            }
        }

        user.setUpdatedAt(OffsetDateTime.now());
        return toDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        return toDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id)));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUsername(String username) {
        return toDTO(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username)));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getUserByUserId(Long id) {
        return getUserById(id);
    }

    @Transactional
    public UserResponseDTO updateRole(Long id, Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setRole(role);
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public UserResponseDTO setActiveStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setActive(active);
        return toDTO(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    // ========== Vault Methods ==========

    @Transactional(readOnly = true)
    public VaultStatusDTO getVaultStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        return VaultStatusDTO.builder()
                .vaultEnabled(user.isVaultEnabled())
                .vaultSalt(user.getVaultSalt())
                .build();
    }

    @Transactional
    public VaultStatusDTO enableVault(Long userId, VaultEnableRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.isVaultEnabled()) {
            throw new ValidationException("Vault is already enabled");
        }

        // Validate confirmation text
        if (!VAULT_CONFIRM_TEXT.equals(request.getConfirmation())) {
            throw new ValidationException("Confirmation text does not match");
        }

        // Generate salt and store it (passphrase is NEVER stored)
        byte[] salt = encryptionService.generateSalt();
        String saltBase64 = Base64.getEncoder().encodeToString(salt);

        user.setVaultEnabled(true);
        user.setVaultSalt(saltBase64);
        userRepository.save(user);

        return VaultStatusDTO.builder()
                .vaultEnabled(true)
                .vaultSalt(saltBase64)
                .build();
    }

    @Transactional
    public VaultStatusDTO disableVault(Long userId, VaultDisableRequestDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!user.isVaultEnabled()) {
            throw new ValidationException("Vault is not enabled");
        }

        // Note: Actual re-encryption of v2: data to v1: requires the vault key
        // to be provided in the X-Vault-Key header. This endpoint just marks
        // vault as disabled. A separate background process or client-side
        // re-encryption would be needed to downgrade existing v2: ciphertext.

        user.setVaultEnabled(false);
        user.setVaultSalt(null);
        userRepository.save(user);

        return VaultStatusDTO.builder()
                .vaultEnabled(false)
                .vaultSalt(null)
                .build();
    }

    public UserResponseDTO toDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .vaultEnabled(user.isVaultEnabled())
                .vaultSalt(user.getVaultSalt())
                .build();
    }
}
