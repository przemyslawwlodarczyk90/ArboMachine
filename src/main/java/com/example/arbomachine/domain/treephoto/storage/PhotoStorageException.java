package com.example.arbomachine.domain.treephoto.storage;

/** Blad techniczny storage zdjec (zapis/odczyt/usuniecie pliku albo niepoprawny klucz). */
public class PhotoStorageException extends RuntimeException {
    public PhotoStorageException(String message) {
        super(message);
    }

    public PhotoStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
