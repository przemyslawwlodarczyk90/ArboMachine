package com.example.arbomachine.domain.treework.dto;

import java.time.Instant;

/** Praca przy drzewie zwracana przez API - samo id drzewa, bez zagniezdzonego drzewa. */
public record TreeWorkResponse(
        Long id,
        Long treeId,
        String name,
        String description,
        String mapSymbolCode,
        Instant createdAt,
        Instant updatedAt
) {}
