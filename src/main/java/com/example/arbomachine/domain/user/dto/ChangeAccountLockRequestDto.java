package com.example.arbomachine.domain.user.dto;

import jakarta.validation.constraints.NotNull;

/** Zablokowanie (true) albo odblokowanie (false) konta przez admina. */
public record ChangeAccountLockRequestDto(
        @NotNull(message = "Locked flag is mandatory")
        Boolean lockedByAdmin
) {}
