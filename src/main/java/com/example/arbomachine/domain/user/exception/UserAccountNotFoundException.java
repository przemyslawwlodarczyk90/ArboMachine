package com.example.arbomachine.domain.user.exception;

/** Brak konta o podanym id albo e-mailu (HTTP 404). */
public class UserAccountNotFoundException extends RuntimeException {
    public UserAccountNotFoundException(String message) {
        super(message);
    }
}
