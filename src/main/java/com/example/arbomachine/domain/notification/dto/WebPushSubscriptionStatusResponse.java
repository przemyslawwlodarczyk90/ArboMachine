package com.example.arbomachine.domain.notification.dto;

/** Czy subskrypcja tej przegladarki nalezy do zalogowanego konta (przelacznik "Otrzymuj powiadomienia push"). */
public record WebPushSubscriptionStatusResponse(boolean subscribed) {}
