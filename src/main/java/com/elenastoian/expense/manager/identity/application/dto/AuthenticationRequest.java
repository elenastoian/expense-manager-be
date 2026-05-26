package com.elenastoian.expense.manager.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "Authentication's email cannot be blank")
    @Email(message = "Register's email should be a valid email")
    private String email;

    @NotBlank(message = "Authentication's password cannot be blank")
    private String password;
}
