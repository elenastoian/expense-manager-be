package com.elenastoian.expense.manager.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class RegisterRequest {
    @NotNull(message = "Register's email cannot be null")
    @NotBlank(message = "Register's email cannot be blank")
    @Email(message = "Register's email should be a valid email")
    private String email;

    @NotNull(message = "Register's password cannot be null")
    @NotBlank(message = "Register's password cannot be blank")
    @NotEmpty(message = "Register's password cannot be empty")
    private String password;
}
