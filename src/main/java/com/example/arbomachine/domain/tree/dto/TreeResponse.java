package com.example.arbomachine.domain.tree.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Drzewo zwracane przez API - samo id projektu, bez zagniezdzonego projektu.
 * location == null oznacza, ze lokalizacja drzewa nie jest jeszcze ustalona; mapSymbolCode == null - neutralny marker.
 */
public record TreeResponse(
        Long id,
        Long projectId,
        String inventoryNumber,
        String speciesLatin,
        String speciesPolish,
        Integer trunkCircumferenceCm,
        BigDecimal heightM,
        BigDecimal crownDiameterM,
        String notes,
        String conditionNotes,
        String hazardNotes,
        TreeLocationResponse location,
        String mapSymbolCode,
        Instant createdAt,
        Instant updatedAt
) {}
