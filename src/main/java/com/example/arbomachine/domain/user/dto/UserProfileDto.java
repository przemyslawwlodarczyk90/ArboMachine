package com.example.arbomachine.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

/**
 * Edytowalne dane profilu uzytkownika - przyjmowane przy rejestracji i edycji profilu.
 * Adresu e-mail nie da sie zmienic: przy edycji musi byc taki sam jak w koncie.
 */
@Builder
public record UserProfileDto(
        Long id,

        @NotBlank(message = "First name is mandatory")
        String firstName,

        @NotBlank(message = "Last name is mandatory")
        String lastName,

        @Email(message = "Email should be valid")
        @NotBlank(message = "Email is mandatory")
        String email,

        @Pattern(regexp = "^\\+?[0-9. ()-]{7,25}$", message = "Phone number is invalid")
        String phoneNumber,

        String street,

        /** Polski format kodu pocztowego, np. 00-123. */
        @Pattern(regexp = "\\d{2}-\\d{3}", message = "Postal code is invalid")
        String postalCode,

        String city
) {}
