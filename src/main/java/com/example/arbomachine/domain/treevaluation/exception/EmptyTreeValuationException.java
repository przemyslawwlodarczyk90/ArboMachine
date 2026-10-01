package com.example.arbomachine.domain.treevaluation.exception;

/** Zapis waloryzacji bez zadnej wartosci (HTTP 400) - do usuniecia waloryzacji sluzy DELETE. */
public class EmptyTreeValuationException extends RuntimeException {
    public EmptyTreeValuationException(String message) {
        super(message);
    }
}
