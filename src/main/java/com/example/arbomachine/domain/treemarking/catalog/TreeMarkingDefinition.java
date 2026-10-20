package com.example.arbomachine.domain.treemarking.catalog;

/**
 * Jedno oznaczenie z katalogu V1.
 *
 * @param code        stabilny identyfikator zapisywany w tree_marking.code (np. TO_REMOVE)
 * @param displayName nazwa w UI (po polsku)
 * @param category    grupa w UI
 * @param shortLabel  1-2 znaki rysowane na mapie w kolku obok drzewa (np. "U")
 * @param color       kolor kolka (#RRGGBB); znak zawsze bialy
 */
public record TreeMarkingDefinition(
        String code,
        String displayName,
        TreeMarkingCategory category,
        String shortLabel,
        String color
) {}
