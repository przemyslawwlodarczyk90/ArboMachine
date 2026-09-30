package com.example.arbomachine.domain.mapsymbol.catalog;

/**
 * Jeden symbol z katalogu V1.
 *
 * @param code        stabilny identyfikator techniczny zapisywany w Tree/TreeWork.mapSymbolCode (np. RED_CROSS)
 * @param displayName nazwa pokazywana w UI (po polsku) - opisuje wyglad, nie rodzaj zabiegu
 * @param shape       ksztalt do narysowania
 * @param fillColor   kolor wypelnienia (#RRGGBB)
 * @param strokeColor kolor obrysu (#RRGGBB)
 */
public record MapSymbolDefinition(
        String code,
        String displayName,
        MapSymbolShape shape,
        String fillColor,
        String strokeColor
) {}
