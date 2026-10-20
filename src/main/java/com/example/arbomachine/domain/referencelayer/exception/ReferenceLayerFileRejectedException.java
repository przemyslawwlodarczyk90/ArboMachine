package com.example.arbomachine.domain.referencelayer.exception;

/**
 * Pliku nie da sie przyjac jako warstwy referencyjnej (HTTP 400). code - kod do polskiego komunikatu we frontendzie
 * (UNSUPPORTED_FILE_FORMAT, EMPTY_FILE, FILE_TOO_LARGE, UNREADABLE_FILE, UNSUPPORTED_CRS, INVALID_COORDINATES, NO_FEATURES, TOO_MANY_FEATURES,
 * TOO_MANY_LAYERS), detail - np. nazwa ukladu.
 */
public class ReferenceLayerFileRejectedException extends RuntimeException {
    private final String code;
    private final String detail;

    public ReferenceLayerFileRejectedException(String code, String detail) {
        super("Reference layer rejected: " + code + (detail == null ? "" : " (" + detail + ")"));
        this.code = code;
        this.detail = detail;
    }

    public String code() {
        return code;
    }

    public String detail() {
        return detail;
    }
}
