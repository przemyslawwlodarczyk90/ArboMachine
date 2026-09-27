package com.example.arbomachine.domain.user.dto;

import com.example.arbomachine.domain.user.enums.UserRole;

/** Dane powitalne panelu danej roli - zaczatek, pod ktory beda dochodzic funkcje konkretnych paneli. */
public record RolePanelWelcomeDto(
        UserRole role,
        String firstName,
        String lastName,
        String welcomeMessage
) {}
