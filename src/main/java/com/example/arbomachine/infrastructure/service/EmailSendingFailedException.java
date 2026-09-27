package com.example.arbomachine.infrastructure.service;

/** Serwer SMTP odrzucil wiadomosc albo jest niedostepny (HTTP 503). */
public class EmailSendingFailedException extends RuntimeException {
    public EmailSendingFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
