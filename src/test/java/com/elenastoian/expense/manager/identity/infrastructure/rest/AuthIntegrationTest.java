package com.elenastoian.expense.manager.identity.infrastructure.rest;

import com.elenastoian.expense.manager.identity.application.dto.AuthenticationRequest;
import com.elenastoian.expense.manager.identity.application.dto.RegisterRequest;
import com.elenastoian.expense.manager.identity.application.dto.TokenConfirmationRequest;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.domain.repository.TokenRepository;
import com.elenastoian.expense.manager.identity.infrastructure.persistance.CustomUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CustomUserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void cleanDb() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String registerAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    private MvcResult confirmToken(String jwt) throws Exception {
        return mockMvc.perform(post("/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenConfirmationRequest(jwt))))
                .andReturn();
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_createsUserAndReturns201() throws Exception {
        mockMvc.perform(post("/auth/register")
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
    void register_savesExactlyOneTokenToDatabase() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("tokentest@example.com", "password"))))
                .andExpect(status().isCreated());

        assertThat(tokenRepository.findAll()).hasSize(1);
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        userRepository.save(User.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .enabled(true)
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("existing@example.com", "password123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_blankEmail_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("", "password"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("user@example.com", ""))))
                .andExpect(status().isBadRequest());
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    void authenticate_correctCredentials_returns200() throws Exception {
        userRepository.save(User.builder()
                .email("auth@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .enabled(true)
                .build());

        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("auth@example.com", "correct-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("auth@example.com"))
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void authenticate_wrongPassword_returns401() throws Exception {
        userRepository.save(User.builder()
                .email("auth@example.com")
                .password(passwordEncoder.encode("correct-password"))
                .enabled(true)
                .build());

        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("auth@example.com", "wrong-password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_unknownUser_returns401() throws Exception {
        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("nobody@example.com", "password"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticate_secondLogin_leavesOnlyOneValidToken() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("relogin@example.com", "password"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("relogin@example.com", "password"))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail("relogin@example.com").orElseThrow();
        long validTokens = tokenRepository.findAllValidTokensByUser(user.getId()).size();
        assertThat(validTokens).isEqualTo(1);
    }

    // ── confirm ───────────────────────────────────────────────────────────────

    @Test
    void confirm_freshToken_returnsTrue() throws Exception {
        String jwt = registerAndGetToken("confirm@example.com", "password");

        mockMvc.perform(post("/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenConfirmationRequest(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(true));
    }

    @Test
    void confirm_garbageToken_returnsFalse() throws Exception {
        mockMvc.perform(post("/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenConfirmationRequest("not-a-jwt"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(false));
    }

    @Test
    void confirm_blankToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenConfirmationRequest(""))))
                .andExpect(status().isBadRequest());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_revokesToken_confirmReturnsFalse() throws Exception {
        String jwt = registerAndGetToken("logout@example.com", "password");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TokenConfirmationRequest(jwt))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validation").value(false));
    }

    // ── Protected routes ──────────────────────────────────────────────────────

    @Test
    void anyProtectedRoute_noToken_returns401() throws Exception {
        mockMvc.perform(get("/protected-placeholder"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyProtectedRoute_validJwt_passesSecurityFilter() throws Exception {
        String jwt = registerAndGetToken("protected@example.com", "password");

        // 404 is the expected outcome — the path doesn't exist
        mockMvc.perform(get("/protected-placeholder")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }
}