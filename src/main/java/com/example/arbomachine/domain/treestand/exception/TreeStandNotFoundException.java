package com.example.arbomachine.domain.treestand.exception;

/** Grupa nie istnieje ALBO nalezy do innego projektu (HTTP 404) - jak przy drzewach. */
public class TreeStandNotFoundException extends RuntimeException {
    public TreeStandNotFoundException(String message) {
        super(message);
    }
}
