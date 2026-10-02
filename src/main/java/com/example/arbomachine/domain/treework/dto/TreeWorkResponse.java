package com.example.arbomachine.domain.treework.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Praca przy drzewie zwracana przez API - samo id drzewa, bez zagniezdzonego drzewa. */
public record TreeWorkResponse(
        Long id,
        Long treeId,
        String name,
        String description,
        String mapSymbolCode,
        /** RECOMMENDED / PLANNED / COMPLETED / CANCELLED (etap 28) */
        String status,
        /** LOW / NORMAL / HIGH / URGENT albo null */
        String priority,
        LocalDate plannedDate,
        LocalDate completedDate,
        Instant createdAt,
        Instant updatedAt
) {}
