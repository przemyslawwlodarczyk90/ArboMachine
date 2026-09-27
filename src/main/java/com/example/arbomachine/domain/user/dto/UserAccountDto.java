package com.example.arbomachine.domain.user.dto;

import com.example.arbomachine.domain.user.enums.UserRole;

/** Widok konta zwracany przez API (tylko do odczytu): dane profilu, rola i flagi statusu. */
public record UserAccountDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String street,
        String postalCode,
        String city,
        UserRole role,
        boolean emailConfirmed,
        boolean lockedByAdmin
) {}
