package com.example.arbomachine.domain.notification.push;

/**
 * Tresc powiadomienia push odczytywana przez service worker (public/sw.js): tytul, tekst, trasa aplikacji do
 * otwarcia po kliknieciu i tag (to samo zdarzenie zastepuje poprzednie powiadomienie zamiast dokladac nowe).
 */
public record WebPushMessage(String title, String body, String url, String tag) {}
