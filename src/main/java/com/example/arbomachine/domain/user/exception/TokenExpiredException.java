package com.example.arbomachine.domain.user.exception;

/** Token z linku (potwierdzenie e-maila albo reset hasla) jest przeterminowany (HTTP 400). */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
