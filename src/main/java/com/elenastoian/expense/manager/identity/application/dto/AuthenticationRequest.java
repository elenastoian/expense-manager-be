package com.elenastoian.expense.manager.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @NotNull(message = "Authentication's email cannot be null")
    @NotBlank(message = "Authentication's email cannot be blank")
    @NotEmpty(message = "Authentication's email cannot be empty")
    private String email;

    @NotNull(message = "Authentication's password cannot be null")
    @NotBlank(message = "Authentication's password cannot be blank")
    @NotEmpty(message = "Authentication's password cannot be empty")
    private String password;
}
