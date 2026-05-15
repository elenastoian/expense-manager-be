package com.elenastoian.expense.manager.identity.infrastructure.rest;

import com.elenastoian.expense.manager.identity.application.dto.AuthenticationRequest;
import com.elenastoian.expense.manager.identity.application.dto.RegisterRequest;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.domain.repository.TokenRepository;
import com.elenastoian.expense.manager.identity.infrastructure.persistance.CustomUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Spring Boot integration tests.
 * Starts the complete application context against H2 in-memory DB.
 * Each test is rolled back automatically via @Transactional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Auth — Full Integration Tests")
class AuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomUserRepository userRepository;
    @Autowired private TokenRepository tokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDb() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register — creates user and returns token")
    void register_createsUserAndReturnsToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("new@example.com", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.token").isString());

        assertThat(userRepository.findByEmail("new@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /auth/register — duplicate email — returns 409")
    void register_duplicateEmail_returns409() throws Exception {
        userRepository.save(User.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .enabled(true)
                .build());

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("existing@example.com", "password123"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /auth/register — persists a token record in DB")
    void register_savesTokenToDatabase() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("tokentest@example.com", "password"))))
                .andExpect(status().isCreated());

        assertThat(tokenRepository.findAll()).hasSize(1);
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/authenticate — correct credentials — returns 200 with token")
    void authenticate_correctCredentials_returns200() throws Exception {
        userRepository.save(User.builder()
                .email("auth@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .enabled(true)
                .build());

        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("auth@example.com", "correct-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("auth@example.com"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("POST /auth/authenticate — wrong password — returns 401")
    void authenticate_wrongPassword_returns401() throws Exception {
        userRepository.save(User.builder()
                .email("auth@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .enabled(true)
                .build());

        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("auth@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/authenticate — non-existent user — returns 401")
    void authenticate_unknownUser_returns401() throws Exception {
        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("nobody@example.com", "password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/authenticate — re-login revokes previous token, issues new one")
    void authenticate_secondLogin_revokesPreviousToken() throws Exception {
        String registerJson = objectMapper.writeValueAsString(
                new RegisterRequest("relogin@example.com", "password"));

        // First login via register
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        long tokenCountAfterRegister = tokenRepository.count();

        // Second login via authenticate
        mockMvc.perform(post("/auth/authenticate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("relogin@example.com", "password"))))
                .andExpect(status().isOk());

        // One new token saved, the old one revoked — count stays the same or is +1
        // The old token is still in DB (revoked=true), new one is added
        assertThat(tokenRepository.count()).isGreaterThanOrEqualTo(tokenCountAfterRegister);

        // Only 1 valid (non-revoked, non-expired) token should exist for this user
        User user = userRepository.findByEmail("relogin@example.com").orElseThrow();
        long validTokens = tokenRepository.findAllValidTokensByUser(user.getId()).size();
        assertThat(validTokens).isEqualTo(1);
    }

    // ── confirmToken ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/confirm — token from register — returns validation=true")
    void confirm_freshToken_returnsTrue() throws Exception {
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("confirm@example.com", "password"))))
                .andExpect(status().isCreated())
                .andReturn();

        String jwt = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        mockMvc.perform(get("/auth/confirm").param("token", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(true));
    }

    @Test
    @DisplayName("GET /auth/confirm — garbage token — returns validation=false")
    void confirm_garbageToken_returnsFalse() throws Exception {
        mockMvc.perform(get("/auth/confirm").param("token", "not-a-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(false));
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/logout — revokes token; subsequent confirm returns false")
    void logout_revokesToken_confirmReturnsFalse() throws Exception {
        // Register to get a token
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("logout@example.com", "password"))))
                .andExpect(status().isCreated())
                .andReturn();

        String jwt = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("token").asText();

        // Logout
        mockMvc.perform(post("/auth/logout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        // Token should now be invalid
        mockMvc.perform(get("/auth/confirm").param("token", jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(false));
    }

    // ── Protected endpoint ────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /some-protected-endpoint — no token — returns 401/403")
    void protectedEndpoint_noToken_returns401or403() throws Exception {
        mockMvc.perform(get("/invoices"))   // a route that should be protected
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
}
