package com.example.arbomachine.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Prosba o wyslanie linku do resetu hasla na podany adres. */
public record PasswordResetRequestDto(
        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is mandatory")
        String email
) {}
