package com.example.arbomachine.domain.treephoto.exception;

/** Splaszczony obraz adnotacji nie jest plikiem PNG albo go brak (HTTP 415). */
public class InvalidRenderedAnnotationImageException extends RuntimeException {
    public InvalidRenderedAnnotationImageException(String message) {
        super(message);
    }
}
