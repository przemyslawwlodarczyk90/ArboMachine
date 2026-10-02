package com.example.arbomachine.domain.notification.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * Testowa "przegladarka": klucze subskrypcji (p256dh, auth) i odszyfrowanie wiadomosci Web Push wg RFC 8291 -
 * niezaleznie od WebPushPayloadEncryptor (tylko HKDF wspolny), zeby testy API sprawdzaly prawdziwa tresc push.
 */
public final class FakeBrowserPushSubscription {

    private final KeyPair keyPair = P256KeyCodec.generateKeyPair();
    private final byte[] authSecret = new byte[16];

    public FakeBrowserPushSubscription() {
        new SecureRandom().nextBytes(authSecret);
    }

    public String p256dh() {
        return P256KeyCodec.base64Url(P256KeyCodec.encodePublicKey((ECPublicKey) keyPair.getPublic()));
    }

    public String auth() {
        return P256KeyCodec.base64Url(authSecret);
    }

    public String decrypt(byte[] body) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        buffer.get(salt);
        buffer.getInt();
        byte[] serverPublic = new byte[buffer.get()];
        buffer.get(serverPublic);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        byte[] uaPublic = P256KeyCodec.encodePublicKey((ECPublicKey) keyPair.getPublic());

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init((ECPrivateKey) keyPair.getPrivate());
        agreement.doPhase(P256KeyCodec.decodePublicKey(serverPublic), true);
        byte[] keyInfo = ByteBuffer.allocate(14 + 65 + 65).put("WebPush: info\0".getBytes(StandardCharsets.US_ASCII))
                .put(uaPublic).put(serverPublic).array();
        byte[] ikm = WebPushPayloadEncryptor.hkdf(authSecret, agreement.generateSecret(), keyInfo, 32);
        byte[] cek = WebPushPayloadEncryptor.hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = WebPushPayloadEncryptor.hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);
        return new String(Arrays.copyOf(padded, padded.length - 1), StandardCharsets.UTF_8);
    }
}
