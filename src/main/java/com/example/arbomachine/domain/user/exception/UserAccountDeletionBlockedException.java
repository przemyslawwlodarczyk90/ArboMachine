package com.example.arbomachine.domain.user.exception;

/** Usuniecie konta zablokowane przez modul, do ktorego naleza dane tego konta, np. projekty (HTTP 409). */
public class UserAccountDeletionBlockedException extends RuntimeException {
    public UserAccountDeletionBlockedException(String message) {
        super(message);
    }
}
