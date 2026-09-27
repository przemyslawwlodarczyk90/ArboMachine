package com.example.arbomachine.domain.user.exception;

/** Token z linku nie istnieje - bledny albo juz wykorzystany (HTTP 404). */
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException(String message) {
        super(message);
    }
}
