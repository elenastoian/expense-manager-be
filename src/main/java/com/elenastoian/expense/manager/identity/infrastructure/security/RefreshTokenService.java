package com.elenastoian.expense.manager.identity.infrastructure.security;

import com.elenastoian.expense.manager.identity.domain.model.RefreshToken;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /** Persists an active refresh token identifier linked to a user. */
    public void saveRefreshToken(User user, String tokenId) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenId(tokenId)
                .revoked(false)
                .expired(false)
                .build();
        refreshTokenRepository.save(token);
    }

    /** Looks up a refresh token record by its unique jti. */
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        return refreshTokenRepository.findByTokenId(tokenId);
    }

    /** Revokes a single refresh token string. */
    public void revokeToken(String tokenId) {
        refreshTokenRepository.findByTokenId(tokenId).ifPresent(token -> {
            token.setRevoked(true);
            token.setExpired(true);
            refreshTokenRepository.save(token);
        });
    }

    /** Revokes all refresh tokens for a user (used during a token rotation security breach). */
    public void revokeAllUserRefreshTokens(User user) {
        List<RefreshToken> validTokens = refreshTokenRepository.findAllValidTokensByUser(user.getId());
        if (validTokens.isEmpty()) return;
        validTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        refreshTokenRepository.saveAll(validTokens);
    }
}