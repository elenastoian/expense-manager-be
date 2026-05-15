package com.elenastoian.expense.manager.identity.application.dto;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private Long id;
    private String email;
    private String token;
}