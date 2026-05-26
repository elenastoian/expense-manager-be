package com.elenastoian.expense.manager.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenConfirmationRequest {

    @NotBlank
    private String token;
}
