package com.example.arbomachine.domain.treemarking.dto;

/** Oznaczenie z katalogu V1 (GET /api/tree-markings): kod, nazwa, grupa (kod i nazwa), skrot na mape, kolor. */
public record TreeMarkingDefinitionResponse(
        String code,
        String displayName,
        String category,
        String categoryDisplayName,
        String shortLabel,
        String color
) {}
