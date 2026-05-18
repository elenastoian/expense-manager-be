package com.elenastoian.expense.manager.identity.infrastructure.security;

import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.domain.model.Token;
import com.elenastoian.expense.manager.identity.domain.model.enums.TokenType;
import com.elenastoian.expense.manager.identity.domain.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

    /**
     * Persists a new valid token record for the given user.
     *
     * @param user    the authenticated user
     * @param tokenId the jti UUID extracted from the generated JWT
     */
    public void saveToken(User user, String tokenId) {
        Token token = Token.builder()
                .user(user)
                .tokenId(tokenId)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    /**
     * Marks all active tokens for a user as expired and revoked.
     * Called before issuing a new token to prevent session accumulation.
     */
    public void revokeAllUserTokens(User user) {
        List<Token> validTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if (validTokens.isEmpty()) return;
        validTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    /**
     * Looks up a token record by its jti claim.
     * Used by the JWT filter to check revocation status.
     */
    public Optional<Token> findByTokenId(String tokenId) {
        return tokenRepository.findByTokenId(tokenId);
    }

    /**
     * Revokes a single token by its jti claim.
     * Called on logout.
     */
    public void revokeToken(String tokenId) {
        tokenRepository.findByTokenId(tokenId).ifPresent(token -> {
            token.setRevoked(true);
            token.setExpired(true);
            tokenRepository.save(token);
        });
    }
}
