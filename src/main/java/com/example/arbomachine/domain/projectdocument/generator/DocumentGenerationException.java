package com.example.arbomachine.domain.projectdocument.generator;

/** Blad techniczny renderowania dokumentu (szablon, PDF) - HTTP 500 z ogolnym komunikatem. */
public class DocumentGenerationException extends RuntimeException {
    public DocumentGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
