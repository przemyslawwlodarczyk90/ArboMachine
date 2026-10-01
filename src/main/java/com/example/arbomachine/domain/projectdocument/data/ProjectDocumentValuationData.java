package com.example.arbomachine.domain.projectdocument.data;

/**
 * Waloryzacja drzewa w dokumencie (etap 27) - ocena arborysty skopiowana bez zmian (bez obliczen).
 * overallCategory: kod TreeValuationCategory albo null ("Nie oceniono"); pozostale pola - opisy albo null.
 */
public record ProjectDocumentValuationData(
        String overallCategory,
        String ageValue,
        String naturalValue,
        String ecologicalValue,
        String culturalValue,
        String landscapeValue,
        String justification,
        String notes
) {}
