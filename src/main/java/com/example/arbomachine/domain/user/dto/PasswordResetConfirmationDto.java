package com.example.arbomachine.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Ustawienie nowego hasla: token z linku w mailu + nowe haslo. */
public record PasswordResetConfirmationDto(
        @NotBlank(message = "Token is mandatory")
        String resetToken,

        @NotBlank(message = "Password is mandatory")
        @Size(min = 8, message = "Password must have at least 8 characters")
        String newPassword
) {}
