package com.example.arbomachine.domain.user.dto;

/** Liczniki kont pokazywane na pulpicie admina. */
public record UserAccountStatisticsDto(
        long totalAccounts,
        long arboristAccounts,
        long officialAccounts,
        long adminAccounts,
        long accountsAwaitingEmailConfirmation,
        long accountsLockedByAdmin
) {}
