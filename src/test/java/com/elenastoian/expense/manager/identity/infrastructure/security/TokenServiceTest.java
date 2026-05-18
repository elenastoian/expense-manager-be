package com.elenastoian.expense.manager.identity.infrastructure.security;

import com.elenastoian.expense.manager.identity.domain.model.Token;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.domain.model.enums.TokenType;
import com.elenastoian.expense.manager.identity.domain.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenService — Unit Tests")
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenService tokenService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("hashed")
                .enabled(true)
                .build();
    }

    // ── saveToken ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveToken persists a new non-revoked, non-expired BEARER token")
    void saveToken_persistsCorrectToken() {
        String tokenId = UUID.randomUUID().toString();
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);

        tokenService.saveToken(user, tokenId);

        verify(tokenRepository).save(captor.capture());
        Token saved = captor.getValue();

        assertThat(saved.getTokenId()).isEqualTo(tokenId);
        assertThat(saved.getTokenType()).isEqualTo(TokenType.BEARER);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.isExpired()).isFalse();
        assertThat(saved.getUser()).isEqualTo(user);
    }

    // ── revokeAllUserTokens ──────────────────────────────────────────────────

    @Test
    @DisplayName("revokeAllUserTokens marks all valid tokens as expired and revoked")
    void revokeAllUserTokens_revokesValidTokens() {
        Token t1 = Token.builder().tokenId("jti-1").revoked(false).expired(false).user(user).tokenType(TokenType.BEARER).build();
        Token t2 = Token.builder().tokenId("jti-2").revoked(false).expired(false).user(user).tokenType(TokenType.BEARER).build();
        when(tokenRepository.findAllValidTokensByUser(user.getId())).thenReturn(List.of(t1, t2));

        tokenService.revokeAllUserTokens(user);

        assertThat(t1.isRevoked()).isTrue();
        assertThat(t1.isExpired()).isTrue();
        assertThat(t2.isRevoked()).isTrue();
        assertThat(t2.isExpired()).isTrue();
        verify(tokenRepository).saveAll(List.of(t1, t2));
    }

    @Test
    @DisplayName("revokeAllUserTokens does nothing when no valid tokens exist")
    void revokeAllUserTokens_noTokens_doesNothing() {
        when(tokenRepository.findAllValidTokensByUser(user.getId())).thenReturn(List.of());

        tokenService.revokeAllUserTokens(user);

        verify(tokenRepository, never()).saveAll(any());
    }

    // ── findByTokenId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByTokenId returns the token when found")
    void findByTokenId_found_returnsToken() {
        String tokenId = "some-jti";
        Token token = Token.builder().tokenId(tokenId).build();
        when(tokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(token));

        Optional<Token> result = tokenService.findByTokenId(tokenId);

        assertThat(result).isPresent().contains(token);
    }

    @Test
    @DisplayName("findByTokenId returns empty when token not found")
    void findByTokenId_notFound_returnsEmpty() {
        when(tokenRepository.findByTokenId(any())).thenReturn(Optional.empty());

        assertThat(tokenService.findByTokenId("missing")).isEmpty();
    }

    // ── revokeToken ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("revokeToken marks the token as revoked and expired, then saves it")
    void revokeToken_revokesAndSavesToken() {
        String tokenId = "jti-to-revoke";
        Token token = Token.builder().tokenId(tokenId).revoked(false).expired(false).build();
        when(tokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(token));

        tokenService.revokeToken(tokenId);

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isExpired()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("revokeToken does nothing when the jti is not found")
    void revokeToken_notFound_doesNothing() {
        when(tokenRepository.findByTokenId(any())).thenReturn(Optional.empty());

        tokenService.revokeToken("ghost-jti");

        verify(tokenRepository, never()).save(any());
    }
}
