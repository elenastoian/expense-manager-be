package com.elenastoian.expense.manager.identity.infrastructure.security;

import com.elenastoian.expense.manager.identity.domain.model.RefreshToken;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * FIX (DB expiry): how long a refresh token lives, read from the same property
     * used by JwtService so both the JWT exp claim and the DB record stay in sync.
     */
    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Value("${max.refresh.tokens.per.user}")
    private int maxTokensPerUser;

    /**
     * Persists an active refresh-token record for a user.
     *
     * (session cap): if the user already has maxTokensPerUser valid sessions,
     *                    the oldest one is revoked before saving the new one.
     */
    @Transactional
    public void saveRefreshToken(User user, String tokenId) {
        this.enforceSessionCap(user);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenId(tokenId)
                .revoked(false)
                .expired(false)
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000L))
                .build();

        refreshTokenRepository.save(token);
    }

    /**
     * Enforces the session cap by revoking the oldest valid refresh
     * tokens if the user already has maxTokensPerUser - 1 active sessions.
     * @param user
     */
    private void enforceSessionCap(User user) {
        List<RefreshToken> active = refreshTokenRepository.findAllValidTokensByUser(user.getId());
        int overflow = active.size() - (maxTokensPerUser - 1);
        if (overflow <= 0) return;

        active.stream()
                .sorted(Comparator.comparingLong(RefreshToken::getId))
                .limit(overflow)
                .forEach(t -> {
                    t.setRevoked(true);
                    t.setExpired(true);
                });

        refreshTokenRepository.saveAll(active);
    }

    /** Looks up a refresh-token record by its unique jti. */
    public Optional<RefreshToken> findByTokenId(String tokenId) {
        return refreshTokenRepository.findByTokenId(tokenId);
    }

    /**
     * Revokes a single token by jti.
     * @param tokenId
     */
    @Transactional
    public void revokeToken(String tokenId) {
        refreshTokenRepository.findByTokenId(tokenId).ifPresent(token -> {
            token.setRevoked(true);
            token.setExpired(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Revokes all valid refresh tokens for a user (breach response).
     * @param user
     */
    @Transactional
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