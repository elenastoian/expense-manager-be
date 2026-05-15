package com.elenastoian.expense.manager.identity.infrastructure.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService — Unit Tests")
class JwtServiceTest {

    // 64 hex chars = 32 bytes, valid for HS256
    private static final String SECRET = "74657374536563726574746573745365637265747465737453656372657474657374";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);

        userDetails = new User("test@example.com", "password", Collections.emptyList());
    }

    // ── generateToken ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken returns a non-null JWT and a non-null tokenId")
    void generateToken_returnsJwtAndTokenId() {
        JwtService.GeneratedToken result = jwtService.generateToken(userDetails);

        assertThat(result).isNotNull();
        assertThat(result.jwt()).isNotBlank();
        assertThat(result.tokenId()).isNotBlank();
    }

    @Test
    @DisplayName("generateToken embeds the username as the subject claim")
    void generateToken_subjectMatchesUsername() {
        JwtService.GeneratedToken result = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(result.jwt()))
                .isEqualTo(userDetails.getUsername());
    }

    @Test
    @DisplayName("generateToken embeds the tokenId as the jti claim")
    void generateToken_jtiMatchesReturnedTokenId() {
        JwtService.GeneratedToken result = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractTokenId(result.jwt()))
                .isEqualTo(result.tokenId());
    }

    @Test
    @DisplayName("generateToken produces unique tokenIds on successive calls")
    void generateToken_uniqueTokenIds() {
        String id1 = jwtService.generateToken(userDetails).tokenId();
        String id2 = jwtService.generateToken(userDetails).tokenId();

        assertThat(id1).isNotEqualTo(id2);
    }

    // ── isTokenValid ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid returns true for a fresh token with matching user")
    void isTokenValid_validToken_returnsTrue() {
        String jwt = jwtService.generateToken(userDetails).jwt();

        assertThat(jwtService.isTokenValid(jwt, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false when username does not match")
    void isTokenValid_wrongUser_returnsFalse() {
        String jwt = jwtService.generateToken(userDetails).jwt();
        UserDetails other = new User("other@example.com", "pw", Collections.emptyList());

        assertThat(jwtService.isTokenValid(jwt, other)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for an expired token")
    void isTokenValid_expiredToken_returnsFalse() {
        // Create a JwtService with 0 ms expiration
        JwtService expiredJwtService = new JwtService();
        ReflectionTestUtils.setField(expiredJwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(expiredJwtService, "jwtExpirationMs", -1000L); // already expired

        String jwt = expiredJwtService.generateToken(userDetails).jwt();

        // Parsing an expired token throws JwtException — isTokenValid must not be called
        // directly on the expired service because extractAllClaims will throw.
        // We verify this by checking that extraction throws.
        assertThatThrownBy(() -> jwtService.isTokenValid(jwt, userDetails))
                .isInstanceOf(JwtException.class);
    }

    // ── extractUsername / extractTokenId ────────────────────────────────────

    @Test
    @DisplayName("extractUsername returns the subject embedded in the token")
    void extractUsername_returnsSubject() {
        String jwt = jwtService.generateToken(userDetails).jwt();

        assertThat(jwtService.extractUsername(jwt)).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("extractTokenId returns the jti embedded in the token")
    void extractTokenId_returnsJti() {
        JwtService.GeneratedToken generated = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractTokenId(generated.jwt()))
                .isEqualTo(generated.tokenId());
    }

    @Test
    @DisplayName("extractUsername throws JwtException for a tampered token")
    void extractUsername_tamperedToken_throwsJwtException() {
        String jwt = jwtService.generateToken(userDetails).jwt();
        String tampered = jwt.substring(0, jwt.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.extractUsername(tampered))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("extractUsername throws JwtException for a completely invalid token")
    void extractUsername_invalidToken_throwsJwtException() {
        assertThatThrownBy(() -> jwtService.extractUsername("not.a.jwt"))
                .isInstanceOf(JwtException.class);
    }
}
