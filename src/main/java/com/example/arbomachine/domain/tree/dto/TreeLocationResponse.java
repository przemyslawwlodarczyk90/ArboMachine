package com.example.arbomachine.domain.tree.dto;

import java.math.BigDecimal;

/**
 * Lokalizacja w TreeResponse (null = drzewo nie ma jeszcze ustalonej lokalizacji). Od etapu 25 z danymi pomiaru:
 * altitudeM / accuracyM w metrach (null = brak) i source (MANUAL, MAP, DEVICE_GNSS, IMPORT; null = punkt sprzed
 * etapu 25, zrodlo nieznane).
 */
public record TreeLocationResponse(
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal altitudeM,
        BigDecimal accuracyM,
        String source
) {}
