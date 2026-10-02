package com.example.arbomachine.domain.notification.dto;

import java.time.Instant;

/** Powiadomienie na liscie "Powiadomienia". linkPath - trasa aplikacji do otwarcia (albo null). */
public record UserNotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        String linkPath,
        Instant createdAt
) {}
