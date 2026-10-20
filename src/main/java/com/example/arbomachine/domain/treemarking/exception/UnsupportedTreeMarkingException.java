package com.example.arbomachine.domain.treemarking.exception;

/** Kod oznaczenia spoza katalogu TreeMarkingCatalog (HTTP 400). */
public class UnsupportedTreeMarkingException extends RuntimeException {
    public UnsupportedTreeMarkingException(String message) {
        super(message);
    }
}
