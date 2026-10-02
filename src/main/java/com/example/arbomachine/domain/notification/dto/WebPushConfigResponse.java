package com.example.arbomachine.domain.notification.dto;

/** Klucz publiczny VAPID serwera (applicationServerKey dla PushManager.subscribe), base64url. */
public record WebPushConfigResponse(String publicKey) {}
