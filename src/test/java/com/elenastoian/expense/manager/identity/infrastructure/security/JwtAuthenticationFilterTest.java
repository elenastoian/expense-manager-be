package com.elenastoian.expense.manager.identity.infrastructure.security;

import com.elenastoian.expense.manager.identity.domain.model.Token;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter — Unit Tests")
class JwtAuthenticationFilterTest {

    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;
    @Mock private TokenService tokenService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = new User("user@example.com", "pw", Collections.emptyList());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Missing / malformed header ────────────────────────────────────────────

    @Test
    @DisplayName("No Authorization header — passes through without authenticating")
    void noAuthHeader_passesThroughUnauthenticated() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Authorization header without 'Bearer ' prefix — passes through")
    void noBearerPrefix_passesThroughUnauthenticated() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcjpwYXNz");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Malformed JWT (JwtException) — passes through without authenticating")
    void malformedJwt_passesThroughUnauthenticated() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer not.a.valid.jwt");
        when(jwtService.extractUsername(any())).thenThrow(new JwtException("bad"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ── Valid token path ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Valid, non-revoked token — sets authentication in SecurityContext")
    void validNonRevokedToken_authenticates() throws Exception {
        String jwt = "valid.jwt.token";
        String jti = "some-uuid";
        Token dbToken = Token.builder().tokenId(jti).revoked(false).expired(false).build();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt);
        when(jwtService.extractUsername(jwt)).thenReturn("user@example.com");
        when(jwtService.extractTokenId(jwt)).thenReturn(jti);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);
        when(tokenService.findByTokenId(jti)).thenReturn(Optional.of(dbToken));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("Valid signature but revoked token — does NOT authenticate")
    void revokedToken_doesNotAuthenticate() throws Exception {
        String jwt = "valid.jwt.token";
        String jti = "revoked-uuid";
        Token dbToken = Token.builder().tokenId(jti).revoked(true).expired(true).build();

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt);
        when(jwtService.extractUsername(jwt)).thenReturn("user@example.com");
        when(jwtService.extractTokenId(jwt)).thenReturn(jti);
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);
        when(tokenService.findByTokenId(jti)).thenReturn(Optional.of(dbToken));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Token jti not found in DB — does NOT authenticate")
    void tokenNotInDb_doesNotAuthenticate() throws Exception {
        String jwt = "valid.jwt.token";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt);
        when(jwtService.extractUsername(jwt)).thenReturn("user@example.com");
        when(jwtService.extractTokenId(jwt)).thenReturn("unknown-jti");
        when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(userDetails);
        when(jwtService.isTokenValid(jwt, userDetails)).thenReturn(true);
        when(tokenService.findByTokenId("unknown-jti")).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Already authenticated context — skips re-authentication")
    void alreadyAuthenticated_skipsProcessing() throws Exception {
        String jwt = "valid.jwt.token";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt);
        when(jwtService.extractUsername(jwt)).thenReturn("user@example.com");
        when(jwtService.extractTokenId(jwt)).thenReturn("some-jti");

        // Pre-populate the security context to simulate already-authenticated state
        var existingAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // UserDetailsService must NOT be called again
        verify(userDetailsService, never()).loadUserByUsername(any());
    }
}
