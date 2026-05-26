package com.elenastoian.expense.manager.identity.infrastructure.rest;

import com.elenastoian.expense.manager.identity.application.dto.*;
import com.elenastoian.expense.manager.identity.application.service.AuthenticationService;
import com.elenastoian.expense.manager.identity.infrastructure.security.JwtAuthenticationFilter;
import com.elenastoian.expense.manager.identity.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private AuthenticationProvider authenticationProvider;
    @MockitoBean
    private LogoutHandler logoutHandler;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201() throws Exception {
        AuthenticationResponse body = new AuthenticationResponse(1L, "user@example.com", "jwt-token");
        when(authenticationService.register(any())).thenReturn(ResponseEntity.status(201).body(body));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", "password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void register_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("", "password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void register_isPubliclyAccessible() throws Exception {
        AuthenticationResponse body = new AuthenticationResponse(1L, "user@example.com", "jwt");
        when(authenticationService.register(any())).thenReturn(ResponseEntity.status(201).body(body));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", "password"))))
                .andExpect(status().isCreated());
    }

    // ── POST /auth/authenticate ───────────────────────────────────────────────

    @Test
    void authenticate_validCredentials_returns200() throws Exception {
        AuthenticationResponse body = new AuthenticationResponse(2L, "user@example.com", "jwt-token");
        when(authenticationService.authenticate(any())).thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("user@example.com", "password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void authenticate_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("", "password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticate_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("user@example.com", ""))))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/confirm ────────────────────────────────────────────────────

    @Test
    void confirm_validToken_returns200True() throws Exception {
        when(authenticationService.confirmToken(any()))
                .thenReturn(ResponseEntity.ok(new TokenConfirmationResponse(true)));

        mockMvc.perform(post("/auth/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JwtTokenConfirmationRequest("some-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(true));
    }

    @Test
    void confirm_invalidToken_returns200False() throws Exception {
        when(authenticationService.confirmToken(any()))
                .thenReturn(ResponseEntity.ok(new TokenConfirmationResponse(false)));

        mockMvc.perform(post("/auth/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new JwtTokenConfirmationRequest("bad-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(false));
    }

    @Test
    void confirm_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenConfirmationRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirm_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/auth/confirm")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}