package com.elenastoian.expense.manager.identity.application.service;

import com.elenastoian.expense.manager.identity.application.dto.*;
import com.elenastoian.expense.manager.identity.domain.model.RefreshToken;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.infrastructure.persistance.CustomUserRepository;
import com.elenastoian.expense.manager.identity.infrastructure.security.JwtService;
import com.elenastoian.expense.manager.identity.infrastructure.security.RefreshTokenService;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final CustomUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public ResponseEntity<AuthenticationResponse> register(@Valid RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .build();

        user = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(issueTokenPair(user));
    }

    public ResponseEntity<AuthenticationResponse> authenticate(@Valid AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        return ResponseEntity.ok(issueTokenPair(user));
    }

    /**
     * Validates the refresh token and issues a new stateless access + stateful refresh pair.
     * Incorporates Automatic Breach Detection during Refresh Token Rotation (RTR).
     */
    @Transactional
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid RefreshTokenRequest request) {
        String rawRefreshToken = request.getRefreshToken();

        String tokenId;
        String username;
        try {
            // extract claims
            tokenId  = jwtService.extractTokenId(rawRefreshToken);
            username = jwtService.extractUsername(rawRefreshToken);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token structural claims");
        }

        // Database lookup via token ID (jti claim)
        RefreshToken dbToken = refreshTokenService.findByTokenId(tokenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));

        // breach detection
        if (dbToken.isRevoked()) {
            refreshTokenService.revokeAllUserRefreshTokens(dbToken.getUser());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Security breach: Session compromised.");
        }

        // cryptographic validation and expiration check of the refresh token
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(rawRefreshToken, userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token signature invalid or expired.");
        }

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Consumption: Revoke the used refresh token immediately
        refreshTokenService.revokeToken(tokenId);

        // Generation: Issue a brand new stateless access token and stateful refresh token
        String newAccess = jwtService.generateAccessToken(userDetails);
        JwtService.GeneratedToken newRefresh = jwtService.generateRefreshToken(userDetails);

        refreshTokenService.saveRefreshToken(user, newRefresh.tokenId());

        return ResponseEntity.ok(new RefreshTokenResponse(newAccess, newRefresh.jwt()));
    }

    /** Confirms the validity of an access token completely statelessly. */
    public ResponseEntity<TokenConfirmationResponse> confirmToken(@Valid TokenConfirmationRequest request) {
        String rawToken = request.getToken();
        try {
            String username = jwtService.extractUsername(rawToken);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                boolean isValid = jwtService.isTokenValid(rawToken, userDetails);
                return ResponseEntity.ok(new TokenConfirmationResponse(isValid));
            }
            return ResponseEntity.ok(new TokenConfirmationResponse(false));
        } catch (Exception e) {
            return ResponseEntity.ok(new TokenConfirmationResponse(false));
        }
    }

    /** Generates a fresh stateless access token string and tracks a stateful refresh token. */
    private AuthenticationResponse issueTokenPair(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String access = jwtService.generateAccessToken(userDetails);
        JwtService.GeneratedToken refresh = jwtService.generateRefreshToken(userDetails);

        // Only save the tracking information of the refresh token to the database
        refreshTokenService.saveRefreshToken(user, refresh.tokenId());

        return new AuthenticationResponse(user.getId(), user.getEmail(), access, refresh.jwt());
    }
}