package com.elenastoian.expense.manager.identity.application.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthenticationResponse {
    private Long id;
    private String email;
    private String accessToken;
    private String refreshToken;
}