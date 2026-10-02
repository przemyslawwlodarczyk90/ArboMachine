package com.example.arbomachine.domain.notification.dto;

/** Wynik powiadomienia testowego: liczba urzadzen, ktore przyjela usluga push (0 = push nie dotarl nigdzie). */
public record TestNotificationResponse(int deliveredDeviceCount) {}
