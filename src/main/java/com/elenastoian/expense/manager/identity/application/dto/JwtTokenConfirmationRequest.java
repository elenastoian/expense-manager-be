package com.elenastoian.expense.manager.identity.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@AllArgsConstructor
public class JwtTokenConfirmationRequest {

    @NotNull
    @NotBlank
    private String token;
}
