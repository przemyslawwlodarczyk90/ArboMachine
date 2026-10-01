package com.example.arbomachine.domain.treevaluation.dto;

import java.time.Instant;

/**
 * Waloryzacja drzewa w API. hasValuation = false -> drzewo nie ma jeszcze waloryzacji (pozostale pola null) - GET
 * zwraca wtedy 200 z pustym obiektem zamiast 404, zeby karta drzewa i kopia offline mialy jeden ksztalt odpowiedzi.
 * overallCategory: LOW / MEDIUM / HIGH / VERY_HIGH albo null ("Nie oceniono").
 */
public record TreeValuationResponse(
        Long treeId,
        boolean hasValuation,
        String overallCategory,
        String ageValue,
        String naturalValue,
        String ecologicalValue,
        String culturalValue,
        String landscapeValue,
        String justification,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static TreeValuationResponse emptyFor(Long treeId) {
        return new TreeValuationResponse(treeId, false, null, null, null, null, null, null, null, null, null, null);
    }
}
