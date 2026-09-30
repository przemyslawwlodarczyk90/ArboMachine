package com.example.arbomachine.domain.treephoto.exception;

/** Plik zdjecia wiekszy niz app.tree-photos.max-file-size (HTTP 413). */
public class TreePhotoFileTooLargeException extends RuntimeException {
    public TreePhotoFileTooLargeException(String message) {
        super(message);
    }
}
