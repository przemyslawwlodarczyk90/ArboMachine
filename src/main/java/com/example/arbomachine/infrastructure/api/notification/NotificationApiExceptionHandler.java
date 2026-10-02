package com.example.arbomachine.infrastructure.api.notification;

import com.example.arbomachine.domain.notification.exception.UnsupportedPushSubscriptionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Bledy modulu powiadomien: niedozwolony adres uslugi push albo bledne klucze subskrypcji -> 400. */
@ControllerAdvice(assignableTypes = NotificationController.class)
public class NotificationApiExceptionHandler {

    @ExceptionHandler(UnsupportedPushSubscriptionException.class)
    public ResponseEntity<String> handleUnsupportedSubscription(UnsupportedPushSubscriptionException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
