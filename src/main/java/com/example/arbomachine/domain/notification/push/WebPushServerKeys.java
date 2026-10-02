package com.example.arbomachine.domain.notification.push;

import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

/** Para kluczy VAPID serwera (P-256) + klucz publiczny w postaci dla przegladarki (applicationServerKey). */
public record WebPushServerKeys(ECPublicKey publicKey, ECPrivateKey privateKey) {

    public String publicKeyBase64Url() {
        return P256KeyCodec.base64Url(P256KeyCodec.encodePublicKey(publicKey));
    }
}
