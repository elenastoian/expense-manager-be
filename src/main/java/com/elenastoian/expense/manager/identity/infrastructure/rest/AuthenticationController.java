package com.elenastoian.expense.manager.identity.infrastructure.rest;

import com.elenastoian.expense.manager.identity.application.dto.AuthenticationRequest;
import com.elenastoian.expense.manager.identity.application.dto.RegisterRequest;
import com.elenastoian.expense.manager.identity.application.dto.AuthenticationResponse;
import com.elenastoian.expense.manager.identity.application.dto.TokenConfirmationResponse;
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

    @GetMapping(path = "/confirm")
    public ResponseEntity<TokenConfirmationResponse> confirm(@RequestParam("token") String token){
        return authenticationService.confirmToken(token);
    }
}
