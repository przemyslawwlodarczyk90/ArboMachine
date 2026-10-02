package com.example.arbomachine.domain.treestand.exception;

/** Bledne dane grupy drzew poza walidacja pol (np. drzewo spoza projektu) - HTTP 400. */
public class InvalidTreeStandException extends RuntimeException {
    public InvalidTreeStandException(String message) {
        super(message);
    }
}
