package com.elenastoian.expense.manager.identity.infrastructure.rest;

import com.elenastoian.expense.manager.identity.application.dto.*;
import com.elenastoian.expense.manager.identity.application.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping(value = "/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody @Valid RegisterRequest request){
        return authenticationService.register(request);
    }

    @PostMapping(value = "/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request){
        return authenticationService.authenticate(request);
    }

    @PostMapping(path = "/confirm")
    public ResponseEntity<TokenConfirmationResponse> confirm(@RequestBody @Valid TokenConfirmationRequest tokenConfirmationRequest){
        return authenticationService.confirmToken(tokenConfirmationRequest);
    }
}
