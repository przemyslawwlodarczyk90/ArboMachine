package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.dto.TreeWorkFieldsRequest;

/**
 * Wspolne zasady normalizacji pol pracy - te same przy tworzeniu i edycji: nazwa przycieta, pusty opis zapisywany
 * jako null. Walidacja (wymagalnosc, dlugosci) jest wczesniej - na adnotacjach requestow.
 */
final class TreeWorkFieldsNormalizer {

    private TreeWorkFieldsNormalizer() {
    }

    record NormalizedTreeWorkFields(String name, String description) {
    }

    static NormalizedTreeWorkFields normalizedFieldsOf(TreeWorkFieldsRequest treeWorkFieldsRequest) {
        String description = treeWorkFieldsRequest.description();
        return new NormalizedTreeWorkFields(
                treeWorkFieldsRequest.name().trim(),
                description == null || description.isBlank() ? null : description.trim());
    }
}
