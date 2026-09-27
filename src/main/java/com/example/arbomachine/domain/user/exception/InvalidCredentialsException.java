package com.example.arbomachine.domain.user.exception;

/** Nieznany e-mail albo bledne haslo - celowo ten sam komunikat w obu przypadkach (HTTP 401). */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
