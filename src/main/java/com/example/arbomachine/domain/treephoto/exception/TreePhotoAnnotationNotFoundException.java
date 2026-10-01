package com.example.arbomachine.domain.treephoto.exception;

/** Zdjecie (z URL, w wlasnym projekcie) nie ma adnotacji (HTTP 404). */
public class TreePhotoAnnotationNotFoundException extends RuntimeException {
    public TreePhotoAnnotationNotFoundException(String message) {
        super(message);
    }
}
