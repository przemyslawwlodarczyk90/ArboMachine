package com.example.arbomachine.domain.mapsymbol.dto;

/** Symbol z katalogu V1 zwracany przez GET /api/map-symbols; shape jako tekst (CIRCLE, SQUARE, ...). */
public record MapSymbolResponse(
        String code,
        String displayName,
        String shape,
        String fillColor,
        String strokeColor
) {}
