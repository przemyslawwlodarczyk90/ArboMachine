package com.example.arbomachine.domain.tree.dto;

import java.math.BigDecimal;

/**
 * Lokalizacja w TreeResponse (null = drzewo nie ma jeszcze ustalonej lokalizacji). Osobny obiekt zamiast pol
 * latitude/longitude w TreeResponse - przyszle dane pomiaru (np. dokladnosc, zrodlo) dojda tutaj.
 */
public record TreeLocationResponse(
        BigDecimal latitude,
        BigDecimal longitude
) {}
