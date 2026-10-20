package com.example.arbomachine.domain.referencelayer.exception;

/** Warstwa nie istnieje ALBO nalezy do innego projektu (HTTP 404). */
public class ReferenceLayerNotFoundException extends RuntimeException {
    public ReferenceLayerNotFoundException(String message) {
        super(message);
    }
}
