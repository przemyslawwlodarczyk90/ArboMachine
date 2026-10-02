package com.example.arbomachine.domain.treestand.geometry;

/** Nieprawidlowa geometria obszaru grupy drzew (HTTP 400; przy imporcie - blad obiektu pliku). */
public class InvalidStandGeometryException extends RuntimeException {
    public InvalidStandGeometryException(String message) {
        super(message);
    }
}
