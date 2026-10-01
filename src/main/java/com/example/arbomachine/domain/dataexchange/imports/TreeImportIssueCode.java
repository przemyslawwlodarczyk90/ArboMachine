package com.example.arbomachine.domain.dataexchange.imports;

/**
 * Kody problemow importu drzew. API zwraca kod + wiersz + kolumne, a polskie zdanie sklada frontend
 * (techniczne komunikaty po angielsku, UI po polsku - jak w calym projekcie). Nigdy nazwy wyjatkow Javy.
 */
public enum TreeImportIssueCode {
    // --- caly plik ---
    UNSUPPORTED_FILE_FORMAT,
    FILE_TOO_LARGE,
    EMPTY_FILE,
    UNREADABLE_FILE,
    INVALID_ENCODING,
    INVALID_GEOJSON,
    UNSUPPORTED_CRS,
    MISSING_REQUIRED_COLUMN,
    NO_DATA_ROWS,
    TOO_MANY_ROWS,
    // --- wiersz (blad - wiersz nie moze byc zaimportowany) ---
    REQUIRED_VALUE_MISSING,
    TEXT_TOO_LONG,
    INVALID_NUMBER,
    NOT_A_WHOLE_NUMBER,
    TOO_MANY_DECIMAL_PLACES,
    VALUE_OUT_OF_RANGE,
    INCOMPLETE_LOCATION,
    UNSUPPORTED_GEOMETRY,
    // --- ostrzezenia (import mozliwy) ---
    DUPLICATE_INVENTORY_NUMBER_IN_FILE,
    INVENTORY_NUMBER_EXISTS_IN_PROJECT
}
