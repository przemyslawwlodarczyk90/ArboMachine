package com.example.arbomachine.domain.user.dto;

import com.example.arbomachine.domain.user.enums.AssignableUserRole;
import jakarta.validation.constraints.NotNull;

/** Zmiana roli konta przez admina - tylko ARBORIST albo OFFICIAL (ADMIN nie przejdzie deserializacji). */
public record ChangeUserRoleRequestDto(
        @NotNull(message = "Role is mandatory")
        AssignableUserRole newRole
) {}
