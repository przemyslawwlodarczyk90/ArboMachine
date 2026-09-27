package com.example.arbomachine.domain.user.exception;

/** Proba logowania, zanim uzytkownik kliknal link potwierdzajacy e-mail (HTTP 403). */
public class EmailNotConfirmedException extends RuntimeException {
    public EmailNotConfirmedException(String message) {
        super(message);
    }
}
