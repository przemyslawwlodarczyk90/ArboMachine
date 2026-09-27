package com.example.arbomachine.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Dane logowania: e-mail i haslo jawnym tekstem (porownywane z hashem w bazie). */
public record LoginRequestDto(
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is mandatory")
        String email,

        @NotBlank(message = "Password is mandatory")
        String password
) {}
