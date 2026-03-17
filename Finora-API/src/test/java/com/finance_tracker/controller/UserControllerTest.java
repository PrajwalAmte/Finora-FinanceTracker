package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.UpdateProfileRequestDTO;
import com.finance_tracker.dto.UserResponseDTO;
import com.finance_tracker.dto.VaultDisableRequestDTO;
import com.finance_tracker.dto.VaultEnableRequestDTO;
import com.finance_tracker.dto.VaultStatusDTO;
import com.finance_tracker.model.Role;
import com.finance_tracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private UserService userService;

    private void setAuth(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
    }

    private UserResponseDTO userDTO(Long id) {
        return UserResponseDTO.builder().id(id).username("alice").email("alice@example.com").role(Role.USER).build();
    }

    @Test
    void getCurrentUser_returnsUser() throws Exception {
        setAuth("1");
        when(userService.getUserByUserId(1L)).thenReturn(userDTO(1L));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void updateProfile_updatesUser() throws Exception {
        setAuth("1");
        UpdateProfileRequestDTO req = new UpdateProfileRequestDTO();
        req.setUsername("alice-updated");

        when(userService.updateProfile(eq(1L), any())).thenReturn(userDTO(1L));

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_returnsList() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userDTO(1L)));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void getUserById_returnsUser() throws Exception {
        when(userService.getUserById(2L)).thenReturn(userDTO(2L));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void updateRole_changesRole() throws Exception {
        when(userService.updateRole(eq(1L), eq(Role.ADMIN))).thenReturn(userDTO(1L));

        mockMvc.perform(patch("/api/users/1/role").param("role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void setActiveStatus_updates() throws Exception {
        when(userService.setActiveStatus(eq(1L), eq(false))).thenReturn(userDTO(1L));

        mockMvc.perform(patch("/api/users/1/status").param("active", "false"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUser_returns200() throws Exception {
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isOk());
        verify(userService).deleteUser(1L);
    }

    @Test
    void getVaultStatus_returnsStatus() throws Exception {
        setAuth("1");
        when(userService.getVaultStatus(1L)).thenReturn(VaultStatusDTO.builder().vaultEnabled(true).build());

        mockMvc.perform(get("/api/users/vault/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vaultEnabled").value(true));
    }

    @Test
    void enableVault_returnsUpdatedStatus() throws Exception {
        setAuth("1");
        VaultEnableRequestDTO req = new VaultEnableRequestDTO();
        req.setPassphrase("strongPass123");
        req.setConfirmation("strongPass123");

        when(userService.enableVault(eq(1L), any())).thenReturn(VaultStatusDTO.builder().vaultEnabled(true).build());

        mockMvc.perform(post("/api/users/vault/enable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void disableVault_returnsUpdatedStatus() throws Exception {
        setAuth("1");
        VaultDisableRequestDTO req = new VaultDisableRequestDTO();
        req.setPassphrase("strongPass123");

        when(userService.disableVault(eq(1L), any())).thenReturn(VaultStatusDTO.builder().vaultEnabled(false).build());

        mockMvc.perform(post("/api/users/vault/disable")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
