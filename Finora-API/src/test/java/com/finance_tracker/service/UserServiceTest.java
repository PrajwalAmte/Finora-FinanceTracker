package com.finance_tracker.service;

import com.finance_tracker.dto.*;
import com.finance_tracker.exception.ResourceNotFoundException;
import com.finance_tracker.exception.ValidationException;
import com.finance_tracker.model.Role;
import com.finance_tracker.model.User;
import com.finance_tracker.repository.UserRepository;
import com.finance_tracker.utils.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private FieldEncryptionService encryptionService;

    @InjectMocks
    private UserService userService;

    private User buildUser(Long id, String username, String email) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(email);
        u.setPasswordHash("hashed");
        u.setRole(Role.USER);
        u.setActive(true);
        u.setVaultEnabled(false);
        return u;
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_success_returnsTokenAndUser() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setUsername("Alice");
        req.setEmail("ALICE@test.com");
        req.setPassword("securePass");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("securePass")).thenReturn("hashed");

        User saved = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtService.generateToken(1L)).thenReturn("jwt-token");

        AuthResponseDTO result = userService.register(req);

        assertThat(result.getToken()).isEqualTo("jwt-token");
        assertThat(result.getUser().getUsername()).isEqualTo("alice");
    }

    @Test
    void register_trimAndLowercasesCredentials() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setUsername("  BOB  ");
        req.setEmail("BOB@EXAMPLE.COM");
        req.setPassword("pass1234");

        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        User saved = buildUser(2L, "bob", "bob@example.com");
        when(userRepository.save(any())).thenReturn(saved);
        when(jwtService.generateToken(2L)).thenReturn("tok");

        userService.register(req);

        verify(userRepository).existsByUsername("bob");
        verify(userRepository).existsByEmail("bob@example.com");
    }

    @Test
    void register_usernameTaken_throws() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("pass");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void register_emailTaken_throws() {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setUsername("alice");
        req.setEmail("alice@test.com");
        req.setPassword("pass");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Email already registered");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_success_updatesLastLoginAndReturnsToken() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("alice@test.com");
        req.setPassword("securePass");

        User user = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("securePass", "hashed")).thenReturn(true);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(1L)).thenReturn("jwt");

        AuthResponseDTO result = userService.login(req);

        assertThat(result.getToken()).isEqualTo("jwt");
        assertThat(user.getLastLoginAt()).isNotNull();
    }

    @Test
    void login_userNotFound_throws() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("unknown@test.com");
        req.setPassword("pass");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_inactiveAccount_throws() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("alice@test.com");
        req.setPassword("pass");

        User user = buildUser(1L, "alice", "alice@test.com");
        user.setActive(false);
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Account is disabled");
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("alice@test.com");
        req.setPassword("wrong");

        User user = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid email or password");
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_changesUsername() {
        UpdateProfileRequestDTO req = new UpdateProfileRequestDTO();
        req.setUsername("newname");

        User user = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        UserResponseDTO result = userService.updateProfile(1L, req);

        assertThat(user.getUsername()).isEqualTo("newname");
    }

    @Test
    void updateProfile_sameUsername_noConflictCheck() {
        UpdateProfileRequestDTO req = new UpdateProfileRequestDTO();
        req.setUsername("alice");

        User user = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.updateProfile(1L, req);

        verify(userRepository, never()).existsByUsername(any());
    }

    @Test
    void updateProfile_newUsernameTaken_throws() {
        UpdateProfileRequestDTO req = new UpdateProfileRequestDTO();
        req.setUsername("taken");

        User user = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Username already taken");
    }

    @Test
    void updateProfile_nullUsername_onlySetsUpdatedAt() {
        UpdateProfileRequestDTO req = new UpdateProfileRequestDTO();

        User user = buildUser(1L, "alice", "alice@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.updateProfile(1L, req);

        assertThat(user.getUsername()).isEqualTo("alice");
    }

    @Test
    void updateProfile_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(99L, new UpdateProfileRequestDTO()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── CRUD operations ───────────────────────────────────────────────────────

    @Test
    void getAllUsers_returnsAllMappedToDTO() {
        when(userRepository.findAll()).thenReturn(List.of(
                buildUser(1L, "alice", "a@test.com"),
                buildUser(2L, "bob", "b@test.com")));

        assertThat(userService.getAllUsers()).hasSize(2);
    }

    @Test
    void getUserById_found_returnsDTO() {
        User user = buildUser(1L, "alice", "a@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.getUserById(1L).getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserByUsername_found_returnsDTO() {
        User user = buildUser(1L, "alice", "a@test.com");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        assertThat(userService.getUserByUsername("alice").getUsername()).isEqualTo("alice");
    }

    @Test
    void getUserByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername("ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUserByUserId_delegatesToGetUserById() {
        User user = buildUser(1L, "alice", "a@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.getUserByUserId(1L).getId()).isEqualTo(1L);
    }

    @Test
    void updateRole_updatesAndReturnsDTO() {
        User user = buildUser(1L, "alice", "a@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UserResponseDTO result = userService.updateRole(1L, Role.ADMIN);

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void updateRole_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateRole(99L, Role.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setActiveStatus_updatesAndReturns() {
        User user = buildUser(1L, "alice", "a@test.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        userService.setActiveStatus(1L, false);

        assertThat(user.isActive()).isFalse();
    }

    @Test
    void setActiveStatus_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setActiveStatus(99L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUser_success_deletes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Vault operations ──────────────────────────────────────────────────────

    @Test
    void getVaultStatus_returnsVaultInfo() {
        User user = buildUser(1L, "alice", "a@test.com");
        user.setVaultEnabled(true);
        user.setVaultSalt("base64salt");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        VaultStatusDTO status = userService.getVaultStatus(1L);

        assertThat(status.isVaultEnabled()).isTrue();
        assertThat(status.getVaultSalt()).isEqualTo("base64salt");
    }

    @Test
    void getVaultStatus_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getVaultStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void enableVault_success_setsSaltAndEnabled() {
        User user = buildUser(1L, "alice", "a@test.com");
        user.setVaultEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(encryptionService.generateSalt()).thenReturn(new byte[16]);
        when(userRepository.save(any())).thenReturn(user);

        VaultEnableRequestDTO req = new VaultEnableRequestDTO();
        req.setPassphrase("strongPass");
        req.setConfirmation("I understand I will permanently lose all data if I lose this passphrase");

        VaultStatusDTO result = userService.enableVault(1L, req);

        assertThat(result.isVaultEnabled()).isTrue();
        assertThat(result.getVaultSalt()).isNotBlank();
    }

    @Test
    void enableVault_alreadyEnabled_throws() {
        User user = buildUser(1L, "alice", "a@test.com");
        user.setVaultEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        VaultEnableRequestDTO req = new VaultEnableRequestDTO();
        req.setPassphrase("pass");
        req.setConfirmation("any");

        assertThatThrownBy(() -> userService.enableVault(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already enabled");
    }

    @Test
    void enableVault_wrongConfirmation_throws() {
        User user = buildUser(1L, "alice", "a@test.com");
        user.setVaultEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        VaultEnableRequestDTO req = new VaultEnableRequestDTO();
        req.setPassphrase("pass");
        req.setConfirmation("wrong confirmation text");

        assertThatThrownBy(() -> userService.enableVault(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Confirmation text does not match");
    }

    @Test
    void disableVault_success_clearsVault() {
        User user = buildUser(1L, "alice", "a@test.com");
        user.setVaultEnabled(true);
        user.setVaultSalt("somesalt");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        VaultDisableRequestDTO req = new VaultDisableRequestDTO();
        req.setPassphrase("strongPass");

        VaultStatusDTO result = userService.disableVault(1L, req);

        assertThat(result.isVaultEnabled()).isFalse();
        assertThat(result.getVaultSalt()).isNull();
        assertThat(user.isVaultEnabled()).isFalse();
        assertThat(user.getVaultSalt()).isNull();
    }

    @Test
    void disableVault_notEnabled_throws() {
        User user = buildUser(1L, "alice", "a@test.com");
        user.setVaultEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        VaultDisableRequestDTO req = new VaultDisableRequestDTO();
        req.setPassphrase("pass");

        assertThatThrownBy(() -> userService.disableVault(1L, req))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Vault is not enabled");
    }

    @Test
    void disableVault_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        VaultDisableRequestDTO req = new VaultDisableRequestDTO();
        req.setPassphrase("pass");

        assertThatThrownBy(() -> userService.disableVault(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── toDTO ─────────────────────────────────────────────────────────────────

    @Test
    void toDTO_mapsAllFields() {
        User user = buildUser(1L, "alice", "a@test.com");
        OffsetDateTime now = OffsetDateTime.now();
        user.setLastLoginAt(now);
        user.setVaultEnabled(true);
        user.setVaultSalt("salt");

        UserResponseDTO dto = userService.toDTO(user);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("a@test.com");
        assertThat(dto.getRole()).isEqualTo(Role.USER);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.getLastLoginAt()).isEqualTo(now);
        assertThat(dto.isVaultEnabled()).isTrue();
        assertThat(dto.getVaultSalt()).isEqualTo("salt");
    }
}
