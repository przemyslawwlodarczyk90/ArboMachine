package com.example.arbomachine.domain.notification.exception;

/** Subskrypcja z niedozwolonym adresem uslugi push albo blednymi kluczami (HTTP 400). */
public class UnsupportedPushSubscriptionException extends RuntimeException {
    public UnsupportedPushSubscriptionException(String message) {
        super(message);
    }
}
