package com.example.arbomachine.domain.treestand.service.treestandfacade;

/** Normalizacja tekstow grupy drzew: przyciete, pusty = brak. */
final class TreeStandTexts {

    private TreeStandTexts() {
    }

    static String trimmedOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
