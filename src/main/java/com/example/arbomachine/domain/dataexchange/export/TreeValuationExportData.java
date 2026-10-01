package com.example.arbomachine.domain.dataexchange.export;

/** Waloryzacja drzewa w eksporcie (etap 27): kod kategorii V1 (albo null) i opisy arborysty - bez zmian. */
public record TreeValuationExportData(
        String overallCategory,
        String ageValue,
        String naturalValue,
        String ecologicalValue,
        String culturalValue,
        String landscapeValue,
        String justification
) {}
