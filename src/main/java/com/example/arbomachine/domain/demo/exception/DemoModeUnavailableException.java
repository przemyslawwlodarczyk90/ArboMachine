package com.example.arbomachine.domain.demo.exception;

/** Tryb demo jest wylaczony w konfiguracji albo konto demo nie jest gotowe (HTTP 404). */
public class DemoModeUnavailableException extends RuntimeException {
    public DemoModeUnavailableException(String message) {
        super(message);
    }
}
