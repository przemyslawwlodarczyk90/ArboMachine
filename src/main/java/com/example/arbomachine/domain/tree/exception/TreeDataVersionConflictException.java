package com.example.arbomachine.domain.tree.exception;

/** Etap 37: dane drzewa zmieniono w miedzyczasie (inne urzadzenie / karta) - zapis z nieaktualna wersja, HTTP 409. */
public class TreeDataVersionConflictException extends RuntimeException {

    private final long currentDataVersion;

    public TreeDataVersionConflictException(long currentDataVersion) {
        super("Tree data was changed in the meantime (current data version: " + currentDataVersion + ")");
        this.currentDataVersion = currentDataVersion;
    }

    public long currentDataVersion() {
        return currentDataVersion;
    }
}
