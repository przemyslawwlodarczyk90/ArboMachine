package com.example.arbomachine.domain.project.dto;

import java.time.Instant;

/** Projekt zwracany przez API. Bez danych wlasciciela - zawsze jest nim pytajacy uzytkownik. */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        /** etap 29: dane zlecenia (null = nie podano) */
        String clientName,
        String siteAddress,
        java.time.LocalDate orderDate,
        Instant createdAt,
        Instant updatedAt
) {}
