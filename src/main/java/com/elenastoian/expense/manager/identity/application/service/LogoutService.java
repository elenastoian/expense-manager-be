package com.elenastoian.expense.manager.identity.application.service;

import com.elenastoian.expense.manager.identity.infrastructure.security.JwtService;
import com.elenastoian.expense.manager.identity.infrastructure.security.RefreshTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(LogoutService.class);

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Frontend must supply the active Refresh Token via this header during log out requests.
        final String refreshToken = request.getHeader("Refresh-Token");
        if (refreshToken == null || refreshToken.isBlank()) {
            logger.debug("No refresh token provided during logout. Skipping token revocation.");
            return;
        }

        try {
            // Extract the unique token ID (jti claim) from the Refresh Token and revoke it
            String tokenId = jwtService.extractTokenId(refreshToken);
            refreshTokenService.revokeToken(tokenId);
        } catch (JwtException e) {
            logger.debug("Failed to revoke refresh token during logout: {}", e.getMessage());
        }
    }
}