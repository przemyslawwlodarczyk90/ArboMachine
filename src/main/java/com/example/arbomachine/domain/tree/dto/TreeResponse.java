package com.example.arbomachine.domain.tree.dto;

import java.time.Instant;

/** Drzewo zwracane przez API - samo id projektu, bez zagniezdzonego projektu. */
public record TreeResponse(
        Long id,
        Long projectId,
        String inventoryNumber,
        String speciesLatin,
        String speciesPolish,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
