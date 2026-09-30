package com.example.arbomachine.domain.treephoto.exception;

/** Upload bez pliku albo z pustym plikiem (HTTP 400). */
public class TreePhotoFileMissingException extends RuntimeException {
    public TreePhotoFileMissingException(String message) {
        super(message);
    }
}
