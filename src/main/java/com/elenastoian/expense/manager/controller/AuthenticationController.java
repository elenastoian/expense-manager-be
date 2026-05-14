package com.elenastoian.expense.manager.controller;

import com.elenastoian.expense.manager.controller.request.AuthenticationRequest;
import com.elenastoian.expense.manager.controller.request.RegisterRequest;
import com.elenastoian.expense.manager.controller.response.AuthenticationResponse;
import com.elenastoian.expense.manager.controller.response.TokenConfirmationResponse;
import com.elenastoian.expense.manager.service.AuthenticationService;
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
