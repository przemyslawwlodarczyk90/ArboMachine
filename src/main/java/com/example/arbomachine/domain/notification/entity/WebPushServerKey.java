package com.example.arbomachine.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Klucze VAPID serwera wygenerowane przy pierwszym uzyciu, gdy nie podano ich w konfiguracji (app.push.vapid-*).
 * Musza byc stale - zmiana kluczy uniewaznia wszystkie subskrypcje przegladarek. V1: klucz prywatny w bazie bez
 * szyfrowania (DLUG - docelowo sekret poza baza, np. arbomachine-secrets.properties albo vault).
 */
@Entity
@Table(name = "web_push_server_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebPushServerKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nieskompresowany punkt P-256, base64url. */
    @Column(name = "public_key", nullable = false, length = 128)
    private String publicKey;

    /** Liczba d klucza prywatnego P-256, base64url. */
    @Column(name = "private_key", nullable = false, length = 64)
    private String privateKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static WebPushServerKey create(String publicKey, String privateKey, Instant creationTime) {
        WebPushServerKey serverKey = new WebPushServerKey();
        serverKey.publicKey = publicKey;
        serverKey.privateKey = privateKey;
        serverKey.createdAt = creationTime;
        return serverKey;
    }
}
