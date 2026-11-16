package com.example.arbomachine.domain.user.exception;

/** Rejestracja tego typu konta jest chwilowo wylaczona w konfiguracji (HTTP 403). */
public class AccountTypeRegistrationClosedException extends RuntimeException {
    public AccountTypeRegistrationClosedException(String message) {
        super(message);
    }
}
