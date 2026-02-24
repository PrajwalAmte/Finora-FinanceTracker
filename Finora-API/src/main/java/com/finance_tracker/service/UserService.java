package com.finance_tracker.service;

import com.finance_tracker.dto.AuthResponseDTO;
import com.finance_tracker.dto.LoginRequestDTO;
import com.finance_tracker.dto.RegisterRequestDTO;
import com.finance_tracker.dto.UserResponseDTO;
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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

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
        String username = request.getUsername().trim().toLowerCase();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ValidationException("Invalid username or password"));

        if (!user.isActive()) {
            throw new ValidationException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid username or password");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user.getId());

        return AuthResponseDTO.builder()
                .token(token)
                .user(toDTO(user))
                .build();
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
                .build();
    }
}
