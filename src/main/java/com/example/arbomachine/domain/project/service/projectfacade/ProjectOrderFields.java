package com.example.arbomachine.domain.project.service.projectfacade;

/** Normalizacja tekstowych danych zlecenia projektu (etap 29): przyciete, pusty tekst = brak wartosci. */
final class ProjectOrderFields {

    private ProjectOrderFields() {
    }

    static String trimmedOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
