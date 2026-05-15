package com.elenastoian.expense.manager.identity.application.service;

import com.elenastoian.expense.manager.identity.application.dto.AuthenticationRequest;
import com.elenastoian.expense.manager.identity.application.dto.RegisterRequest;
import com.elenastoian.expense.manager.identity.application.dto.AuthenticationResponse;
import com.elenastoian.expense.manager.identity.application.dto.TokenConfirmationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    public ResponseEntity<AuthenticationResponse> register(@Valid RegisterRequest request) {
        return null;
    }

    public ResponseEntity<AuthenticationResponse> authenticate(@Valid AuthenticationRequest request) {
        return null;
    }

    public ResponseEntity<TokenConfirmationResponse> confirmToken(String token) {
        return null;
    }
}