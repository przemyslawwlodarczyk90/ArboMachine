package com.example.arbomachine.domain.notification.push;

import com.example.arbomachine.domain.notification.entity.WebPushServerKey;
import com.example.arbomachine.infrastructure.repository.WebPushServerKeyRepository;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;

/**
 * Klucze VAPID serwera: z konfiguracji (pierwszenstwo), z bazy albo - przy pierwszym uzyciu - nowo wygenerowane
 * i zapisane w bazie. Raz wczytane trzymane w pamieci.
 */
@Component
public class WebPushServerKeyProvider {
    private final WebPushSettings webPushSettings;
    private final WebPushServerKeyRepository webPushServerKeyRepository;
    private volatile WebPushServerKeys cachedKeys;

    public WebPushServerKeyProvider(WebPushSettings webPushSettings, WebPushServerKeyRepository webPushServerKeyRepository) {
        this.webPushSettings = webPushSettings;
        this.webPushServerKeyRepository = webPushServerKeyRepository;
    }

    public WebPushServerKeys serverKeys() {
        WebPushServerKeys keys = cachedKeys;
        if (keys == null) {
            synchronized (this) {
                if (cachedKeys == null) cachedKeys = loadOrCreate();
                keys = cachedKeys;
            }
        }
        return keys;
    }

    private WebPushServerKeys loadOrCreate() {
        if (webPushSettings.hasConfiguredKeys()) {
            return keysOf(webPushSettings.configuredPublicKey(), webPushSettings.configuredPrivateKey());
        }
        WebPushServerKey storedKey = webPushServerKeyRepository.findFirstByOrderByIdAsc().orElseGet(() -> {
            KeyPair keyPair = P256KeyCodec.generateKeyPair();
            webPushServerKeyRepository.save(WebPushServerKey.create(
                    P256KeyCodec.base64Url(P256KeyCodec.encodePublicKey((ECPublicKey) keyPair.getPublic())),
                    P256KeyCodec.base64Url(P256KeyCodec.encodePrivateKey((ECPrivateKey) keyPair.getPrivate())),
                    Instant.now()));
            // Gdyby rownolegle powstal drugi wiersz - wszyscy uzywaja najstarszego.
            return webPushServerKeyRepository.findFirstByOrderByIdAsc().orElseThrow();
        });
        return keysOf(storedKey.getPublicKey(), storedKey.getPrivateKey());
    }

    private static WebPushServerKeys keysOf(String publicKey, String privateKey) {
        return new WebPushServerKeys(P256KeyCodec.decodePublicKey(P256KeyCodec.fromBase64Url(publicKey)),
                P256KeyCodec.decodePrivateKey(P256KeyCodec.fromBase64Url(privateKey)));
    }
}
