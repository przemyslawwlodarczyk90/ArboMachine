package com.example.arbomachine.domain.user.exception;

/** Proba zmiany konta administratora z poziomu aplikacji - konta ADMIN zmienia sie tylko w bazie (HTTP 403). */
public class AdminAccountModificationForbiddenException extends RuntimeException {
    public AdminAccountModificationForbiddenException(String message) {
        super(message);
    }
}
