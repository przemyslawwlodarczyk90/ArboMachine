package com.example.arbomachine.domain.treephoto.exception;

/** Etap 33: identyfikator uploadu z urzadzenia nie jest UUID (HTTP 400). */
public class InvalidClientUploadIdException extends RuntimeException {
    public InvalidClientUploadIdException(String message) {
        super(message);
    }
}
