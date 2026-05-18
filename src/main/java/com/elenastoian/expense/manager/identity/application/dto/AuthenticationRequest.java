package com.elenastoian.expense.manager.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class AuthenticationRequest {

    @NotNull(message = "Authentication's email cannot be null")
    @NotBlank(message = "Authentication's email cannot be blank")
    @NotEmpty(message = "Authentication's email cannot be empty")
    @Email(message = "Register's email should be a valid email")
    private String email;

    @NotNull(message = "Authentication's password cannot be null")
    @NotBlank(message = "Authentication's password cannot be blank")
    @NotEmpty(message = "Authentication's password cannot be empty")
    @NotEmpty(message = "Authentication's password cannot be empty")
    private String password;
}
