package com.elenastoian.expense.manager.identity.application.service;

import com.elenastoian.expense.manager.identity.infrastructure.security.JwtService;
import com.elenastoian.expense.manager.identity.infrastructure.security.TokenService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock private JwtService jwtService;
    @Mock private TokenService tokenService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;

    @InjectMocks
    private LogoutService logoutService;

    @Test
    void validToken_revokesToken() {
        String jwt = "valid.jwt.token";
        String jti = "some-uuid";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + jwt);
        when(jwtService.extractTokenId(jwt)).thenReturn(jti);

        logoutService.logout(request, response, authentication);

        verify(tokenService).revokeToken(jti);
    }

    @Test
    void noAuthHeader_doesNothing() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        logoutService.logout(request, response, authentication);

        verifyNoInteractions(jwtService, tokenService);
    }

    @Test
    void nonBearerHeader_doesNothing() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic abc123");

        logoutService.logout(request, response, authentication);

        verifyNoInteractions(jwtService, tokenService);
    }

    @Test
    void malformedJwt_swallowsException() {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer malformed.token");
        when(jwtService.extractTokenId(any())).thenThrow(new JwtException("bad token"));

        // Must NOT throw
        logoutService.logout(request, response, authentication);

        verify(tokenService, never()).revokeToken(any());
    }
}
