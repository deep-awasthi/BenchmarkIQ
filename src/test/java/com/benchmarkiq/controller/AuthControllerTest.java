package com.benchmarkiq.controller;

import com.benchmarkiq.dto.request.LoginRequest;
import com.benchmarkiq.dto.request.RegisterRequest;
import com.benchmarkiq.dto.response.AuthResponse;
import com.benchmarkiq.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setEmail("new@test.com");
        req.setPassword("Password@1");

        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder().id(1L).username("newuser")
                        .email("new@test.com").role("USER").build())
                .build();

        when(authService.register(any())).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.username").value("newuser"));
    }

    @Test
    void register_invalidRequest_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("u");
        req.setEmail("not-an-email");
        req.setPassword("weak");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void login_validRequest_returns200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin@123");

        AuthResponse response = AuthResponse.builder()
                .accessToken("jwt-token")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(AuthResponse.UserInfo.builder().id(1L).username("admin")
                        .email("admin@test.com").role("ADMIN").build())
                .build();

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
