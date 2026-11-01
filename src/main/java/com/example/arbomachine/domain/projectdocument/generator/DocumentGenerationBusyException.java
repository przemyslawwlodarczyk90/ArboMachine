package com.example.arbomachine.domain.projectdocument.generator;

/** Wszystkie miejsca na generowanie dokumentu zajete dluzej niz dozwolone czekanie - klient ma sprobowac pozniej (503). */
public class DocumentGenerationBusyException extends RuntimeException {
    public DocumentGenerationBusyException(String message) {
        super(message);
    }
}
