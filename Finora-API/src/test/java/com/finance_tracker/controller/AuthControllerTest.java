package com.finance_tracker.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance_tracker.dto.AuthResponseDTO;
import com.finance_tracker.dto.LoginRequestDTO;
import com.finance_tracker.dto.RegisterRequestDTO;
import com.finance_tracker.dto.UserResponseDTO;
import com.finance_tracker.exception.BusinessLogicException;
import com.finance_tracker.service.UserService;
import com.finance_tracker.utils.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import com.finance_tracker.utils.security.JwtService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private JwtService jwtService;


    @MockitoBean
    private UserService userService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    private AuthResponseDTO authResponse() {
        UserResponseDTO user = UserResponseDTO.builder().id(1L).username("alice").email("alice@example.com").build();
        return AuthResponseDTO.builder().token("jwt.token.here").user(user).build();
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO();
        req.setUsername("alice");
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(userService.register(any())).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt.token.here"));
    }

    @Test
    void register_invalidRequest_returns400() throws Exception {
        RegisterRequestDTO req = new RegisterRequestDTO(); // missing fields

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_validCredentials_returns200() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        when(userService.login(any())).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt.token.here"));
    }

    @Test
    void login_rateLimited_returns400() throws Exception {
        LoginRequestDTO req = new LoginRequestDTO();
        req.setEmail("alice@example.com");
        req.setPassword("password123");

        doThrow(new BusinessLogicException("Too many login attempts. Please try again in a minute."))
                .when(loginRateLimiter).checkAndRecord(any());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
