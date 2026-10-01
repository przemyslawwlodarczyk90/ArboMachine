package com.example.arbomachine.domain.treephoto.exception;

/** Dokument adnotacji niezgodny ze schematem V1 (wersja, typy, punkty 0..1, limity) (HTTP 400). */
public class InvalidPhotoAnnotationException extends RuntimeException {
    public InvalidPhotoAnnotationException(String message) {
        super(message);
    }
}
