package com.example.arbomachine.domain.mapsymbol.exception;

/** Kod symbolu spoza katalogu MapSymbolCatalog (HTTP 400). */
public class UnsupportedMapSymbolException extends RuntimeException {
    public UnsupportedMapSymbolException(String message) {
        super(message);
    }
}
