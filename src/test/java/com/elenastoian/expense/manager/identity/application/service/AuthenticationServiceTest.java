package com.elenastoian.expense.manager.identity.application.service;

import com.elenastoian.expense.manager.identity.application.dto.*;
import com.elenastoian.expense.manager.identity.domain.model.Token;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.infrastructure.persistance.CustomUserRepository;
import com.elenastoian.expense.manager.identity.infrastructure.security.JwtService;
import com.elenastoian.expense.manager.identity.infrastructure.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private CustomUserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User savedUser;
    private UserDetails mockUserDetails;
    private JwtService.GeneratedToken generatedToken;

    @BeforeEach
    void setUp() {
        savedUser = User.builder()
                .id(42L)
                .email("user@example.com")
                .password("hashed-password")
                .enabled(true)
                .build();

        mockUserDetails = new org.springframework.security.core.userdetails.User(
                "user@example.com", "hashed-password", Collections.emptyList());

        generatedToken = new JwtService.GeneratedToken("jwt-token-string", UUID.randomUUID().toString());
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_newEmail_returns201() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn(generatedToken);

        RegisterRequest request = new RegisterRequest("user@example.com", "plain-password");
        ResponseEntity<AuthenticationResponse> response = authenticationService.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("user@example.com");
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token-string");
        assertThat(response.getBody().getId()).isEqualTo(42L);
    }

    @Test
    void register_encodesPassword() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any())).thenReturn(generatedToken);

        authenticationService.register(new RegisterRequest("user@example.com", "plain-password"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("hashed-password");
    }

    @Test
    void register_revokesOldTokens() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any())).thenReturn(generatedToken);

        authenticationService.register(new RegisterRequest("user@example.com", "password"));

        verify(tokenService).revokeAllUserTokens(savedUser);
        verify(tokenService).saveToken(savedUser, generatedToken.tokenId());
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(savedUser));

        assertThatThrownBy(() ->
                authenticationService.register(new RegisterRequest("user@example.com", "password")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
    }

    // ── authenticate ──────────────────────────────────────────────────────────

    @Test
    void authenticate_validCredentials_returns200() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(savedUser));
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn(generatedToken);

        ResponseEntity<AuthenticationResponse> response = authenticationService.authenticate(
                new AuthenticationRequest("user@example.com", "plain-password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(42L);
        assertThat(response.getBody().getEmail()).isEqualTo("user@example.com");
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token-string");
    }

    @Test
    void authenticate_revokesOldTokensBeforeIssuing() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(savedUser));
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any())).thenReturn(generatedToken);

        authenticationService.authenticate(new AuthenticationRequest("user@example.com", "password"));

        verify(tokenService).revokeAllUserTokens(savedUser);
        verify(tokenService).saveToken(savedUser, generatedToken.tokenId());
    }

    @Test
    void authenticate_delegatesToAuthenticationManager() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(savedUser));
        when(userDetailsService.loadUserByUsername(any())).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any())).thenReturn(generatedToken);

        authenticationService.authenticate(new AuthenticationRequest("user@example.com", "password"));

        verify(authenticationManager).authenticate(
                argThat(auth -> auth instanceof UsernamePasswordAuthenticationToken
                        && auth.getPrincipal().equals("user@example.com")));
    }

    @Test
    void authenticate_badCredentials_throwsBadCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() ->
                authenticationService.authenticate(new AuthenticationRequest("user@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
    }

    // ── confirmToken ──────────────────────────────────────────────────────────

    @Test
    void confirmToken_validToken_returnsTrue() {
        String jwt = "valid.jwt";
        String jti = "some-jti";
        Token dbToken = Token.builder().tokenId(jti).revoked(false).expired(false).build();

        when(jwtService.extractTokenId(jwt)).thenReturn(jti);
        when(jwtService.extractUsername(jwt)).thenReturn("user@example.com");
        when(tokenService.findByTokenId(jti)).thenReturn(Optional.of(dbToken));
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(mockUserDetails);
        when(jwtService.isTokenValid(jwt, mockUserDetails)).thenReturn(true);

        ResponseEntity<TokenConfirmationResponse> response = authenticationService.confirmToken(new TokenConfirmationRequest(jwt));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValidation()).isTrue();
    }

    @Test
    void confirmToken_revokedToken_returnsFalse() {
        String jwt = "revoked.jwt";
        String jti = "revoked-jti";
        Token dbToken = Token.builder().tokenId(jti).revoked(true).expired(true).build();

        when(jwtService.extractTokenId(jwt)).thenReturn(jti);
        when(tokenService.findByTokenId(jti)).thenReturn(Optional.of(dbToken));

        ResponseEntity<TokenConfirmationResponse> response = authenticationService.confirmToken(new TokenConfirmationRequest(jwt));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValidation()).isFalse();
    }

    @Test
    void confirmToken_malformedJwt_returnsFalse() {
        when(jwtService.extractTokenId(any())).thenThrow(new io.jsonwebtoken.JwtException("bad"));

        ResponseEntity<TokenConfirmationResponse> response = authenticationService.confirmToken(new TokenConfirmationRequest("garbage"));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValidation()).isFalse();
    }

    @Test
    @DisplayName("confirmToken — jti not in DB — returns validation=false")
    void confirmToken_jtiNotInDb_returnsFalse() {
        String jwt = "unknown.jwt";
        when(jwtService.extractTokenId(jwt)).thenReturn("unknown-jti");
        when(jwtService.extractUsername(jwt)).thenReturn("user@example.com");
        when(tokenService.findByTokenId("unknown-jti")).thenReturn(Optional.empty());

        ResponseEntity<TokenConfirmationResponse> response = authenticationService.confirmToken(new TokenConfirmationRequest(jwt));

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isValidation()).isFalse();
    }
}
