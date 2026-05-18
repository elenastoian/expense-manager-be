package com.elenastoian.expense.manager.identity.application.service;

import com.elenastoian.expense.manager.identity.application.dto.*;
import com.elenastoian.expense.manager.identity.domain.model.User;
import com.elenastoian.expense.manager.identity.infrastructure.persistance.CustomUserRepository;
import com.elenastoian.expense.manager.identity.infrastructure.security.JwtService;
import com.elenastoian.expense.manager.identity.infrastructure.security.TokenService;
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
    private final TokenService tokenService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public ResponseEntity<AuthenticationResponse> register(@Valid RegisterRequest request) {
        // Prevent duplicate registrations
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true) // todo: set to false when building email confirmation flow
                .build();

        user = userRepository.save(user);

        // Revoke any stale tokens (shouldn't exist for new user, but defensive)
        tokenService.revokeAllUserTokens(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        JwtService.GeneratedToken generated = jwtService.generateToken(userDetails);
        tokenService.saveToken(user, generated.tokenId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthenticationResponse(user.getId(), user.getEmail(), generated.jwt()));
    }

    public ResponseEntity<AuthenticationResponse> authenticate(@Valid AuthenticationRequest request) {
        // Delegates to DaoAuthenticationProvider — throws AuthenticationException on failure
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        // Revoke all previous tokens before issuing a new one
        tokenService.revokeAllUserTokens(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        JwtService.GeneratedToken generated = jwtService.generateToken(userDetails);
        tokenService.saveToken(user, generated.tokenId());

        return ResponseEntity.ok(new AuthenticationResponse(user.getId(), user.getEmail(), generated.jwt()));
    }

    public ResponseEntity<TokenConfirmationResponse> confirmToken(@Valid TokenConfirmationRequest token) {
        // Extract jti and validate against DB
        try {
            String tokenId = jwtService.extractTokenId(token.getToken());
            boolean isValid = tokenService.findByTokenId(tokenId)
                    .map(t -> !t.isExpired() && !t.isRevoked())
                    .orElse(false);

            // Also verify cryptographic validity
            String username = jwtService.extractUsername(token.getToken());
            if (isValid && username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                isValid = jwtService.isTokenValid(token.getToken(), userDetails);
            } else {
                isValid = false;
            }

            return ResponseEntity.ok(new TokenConfirmationResponse(isValid));
        } catch (Exception e) {
            return ResponseEntity.ok(new TokenConfirmationResponse(false));
        }
    }
}