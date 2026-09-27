package com.example.arbomachine.domain.user.exception;

/** Proba logowania na konto zablokowane przez admina (HTTP 403). */
public class AccountLockedByAdminException extends RuntimeException {
    public AccountLockedByAdminException(String message) {
        super(message);
    }
}
