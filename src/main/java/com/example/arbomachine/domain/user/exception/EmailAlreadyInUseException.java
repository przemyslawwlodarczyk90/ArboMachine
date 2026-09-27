package com.example.arbomachine.domain.user.exception;

/** Rejestracja na adres, ktory ma juz potwierdzone albo zablokowane konto (HTTP 409). */
public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(String message) {
        super(message);
    }
}
