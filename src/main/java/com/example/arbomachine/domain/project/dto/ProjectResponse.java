package com.example.arbomachine.domain.project.dto;

import java.time.Instant;

/** Projekt zwracany przez API. Bez danych wlasciciela - zawsze jest nim pytajacy uzytkownik. */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
