package com.elenastoian.expense.manager.service;

import com.elenastoian.expense.manager.controller.request.AuthenticationRequest;
import com.elenastoian.expense.manager.controller.request.RegisterRequest;
import com.elenastoian.expense.manager.controller.response.AuthenticationResponse;
import com.elenastoian.expense.manager.controller.response.TokenConfirmationResponse;
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