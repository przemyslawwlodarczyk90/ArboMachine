package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.dto.TreeEditableFieldsRequest;
import com.example.arbomachine.domain.tree.entity.TreeEditableAttributes;

/**
 * Wspolne, proste zasady normalizacji pol drzewa - te same przy tworzeniu i edycji: numer inwentaryzacyjny
 * przyciety, puste pola opcjonalne zapisywane jako null. Celowo nie jest to rozbudowany normalizator danych.
 * Walidacja (wymagalnosc, dlugosci, zakresy) jest wczesniej - na adnotacjach requestow.
 */
final class TreeEditableAttributesNormalizer {

    private TreeEditableAttributesNormalizer() {
    }

    static TreeEditableAttributes normalizedAttributesOf(TreeEditableFieldsRequest treeEditableFieldsRequest) {
        return new TreeEditableAttributes(
                treeEditableFieldsRequest.inventoryNumber().trim(),
                trimmedOrNullWhenBlank(treeEditableFieldsRequest.speciesLatin()),
                trimmedOrNullWhenBlank(treeEditableFieldsRequest.speciesPolish()),
                trimmedOrNullWhenBlank(treeEditableFieldsRequest.notes())
        );
    }

    private static String trimmedOrNullWhenBlank(String optionalText) {
        return optionalText == null || optionalText.isBlank() ? null : optionalText.trim();
    }
}
