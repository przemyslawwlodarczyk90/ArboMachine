package com.example.arbomachine.domain.tree.service.treefacade;

/**
 * Wspolne, proste zasady tekstu drzewa - te same przy tworzeniu i edycji: numer inwentaryzacyjny przyciety,
 * puste pola opcjonalne zapisywane jako null. Celowo nie jest to rozbudowany normalizator danych.
 */
final class TreeTextNormalizer {

    private TreeTextNormalizer() {
    }

    static String trimmedInventoryNumber(String inventoryNumber) {
        return inventoryNumber.trim();
    }

    static String trimmedOrNullWhenBlank(String optionalText) {
        return optionalText == null || optionalText.isBlank() ? null : optionalText.trim();
    }
}
