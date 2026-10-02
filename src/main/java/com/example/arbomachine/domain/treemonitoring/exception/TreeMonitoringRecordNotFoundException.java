package com.example.arbomachine.domain.treemonitoring.exception;

/** Wpis nie istnieje ALBO nalezy do innego drzewa niz to z URL (HTTP 404) - jak przy pracach. */
public class TreeMonitoringRecordNotFoundException extends RuntimeException {
    public TreeMonitoringRecordNotFoundException(String message) {
        super(message);
    }
}
