package com.elenastoian.expense.manager.controller.response;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private Long id;
    private String email;
    private String token;
}