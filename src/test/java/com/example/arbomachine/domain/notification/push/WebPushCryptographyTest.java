package com.example.arbomachine.domain.notification.push;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Szyfrowanie Web Push (RFC 8291) i VAPID (RFC 8292) bez bibliotek zewnetrznych - wektor z RFC i proba odszyfrowania. */
class WebPushCryptographyTest {

    // RFC 8291, dodatek A (base64url, bez spacji z dokumentu).
    private static final String PLAINTEXT = "V2hlbiBJIGdyb3cgdXAsIEkgd2FudCB0byBiZSBhIHdhdGVybWVsb24";
    private static final String AS_PUBLIC = "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String AS_PRIVATE = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw";
    private static final String UA_PUBLIC = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String SALT = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String AUTH_SECRET = "BTBZMqHH6r4Tts7J_aSIgg";
    private static final String EXPECTED_HEADER = "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String EXPECTED_CIPHERTEXT = "8pfeW0KbunFT06SuDKoJH9Ql87S1QUrdirN6GcG7sFz1y1sqLgVi1VhjVkHsUoEsbI_0LpXMuGvnzQ";

    private final WebPushPayloadEncryptor encryptor = new WebPushPayloadEncryptor();

    @Test
    void encryptionMatchesRfc8291Example() {
        KeyPair serverKeys = new KeyPair(P256KeyCodec.decodePublicKey(P256KeyCodec.fromBase64Url(AS_PUBLIC)),
                P256KeyCodec.decodePrivateKey(P256KeyCodec.fromBase64Url(AS_PRIVATE)));
        byte[] body = encryptor.encrypt(P256KeyCodec.fromBase64Url(PLAINTEXT), P256KeyCodec.fromBase64Url(UA_PUBLIC),
                P256KeyCodec.fromBase64Url(AUTH_SECRET), serverKeys, P256KeyCodec.fromBase64Url(SALT));
        byte[] header = Arrays.copyOf(body, 86);
        byte[] ciphertext = Arrays.copyOfRange(body, 86, body.length);
        assertThat(P256KeyCodec.base64Url(header)).isEqualTo(EXPECTED_HEADER);
        assertThat(P256KeyCodec.base64Url(ciphertext)).isEqualTo(EXPECTED_CIPHERTEXT);
    }

    @Test
    void userAgentCanDecryptRandomizedMessage() throws Exception {
        KeyPair userAgentKeys = P256KeyCodec.generateKeyPair();
        byte[] authSecret = new byte[16];
        new java.security.SecureRandom().nextBytes(authSecret);
        byte[] uaPublic = P256KeyCodec.encodePublicKey((ECPublicKey) userAgentKeys.getPublic());
        String message = "{\"title\":\"Drzewo G-1: stan zły\",\"body\":\"Ćma bukszpanowa\"}";

        byte[] body = encryptor.encrypt(message.getBytes(StandardCharsets.UTF_8), uaPublic, authSecret);

        assertThat(new String(decryptAsUserAgent(body, (ECPrivateKey) userAgentKeys.getPrivate(), uaPublic, authSecret),
                StandardCharsets.UTF_8)).isEqualTo(message);
        assertThat(encryptor.encrypt(message.getBytes(StandardCharsets.UTF_8), uaPublic, authSecret))
                .as("losowa sol i klucze - kazda wiadomosc inna").isNotEqualTo(body);
    }

    @Test
    void invalidSubscriptionKeysAreRejected() {
        byte[] notOnCurve = new byte[65];
        notOnCurve[0] = 0x04;
        notOnCurve[64] = 1;
        assertThatThrownBy(() -> P256KeyCodec.decodePublicKey(notOnCurve)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> P256KeyCodec.decodePublicKey(new byte[33])).isInstanceOf(IllegalArgumentException.class);
        byte[] uaPublic = P256KeyCodec.fromBase64Url(UA_PUBLIC);
        assertThatThrownBy(() -> encryptor.encrypt(new byte[1], uaPublic, new byte[8])).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vapidTokenIsSignedEs256WithAudienceOfEndpoint() throws Exception {
        KeyPair keyPair = P256KeyCodec.generateKeyPair();
        WebPushServerKeys serverKeys = new WebPushServerKeys((ECPublicKey) keyPair.getPublic(), (ECPrivateKey) keyPair.getPrivate());
        Instant now = Instant.parse("2026-10-02T10:00:00Z");
        String header = new VapidAuthorizationFactory(new ObjectMapper())
                .authorizationHeaderFor(URI.create("https://fcm.googleapis.com/fcm/send/abc"), serverKeys, "mailto:test@example.com", now);

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + serverKeys.publicKeyBase64Url());
        String token = header.substring("vapid t=".length(), header.indexOf(", k="));
        String[] parts = token.split("\\.");
        JsonNode claims = new ObjectMapper().readTree(P256KeyCodec.fromBase64Url(parts[1]));
        assertThat(claims.get("aud").asText()).isEqualTo("https://fcm.googleapis.com");
        assertThat(claims.get("sub").asText()).isEqualTo("mailto:test@example.com");
        assertThat(claims.get("exp").asLong()).isEqualTo(now.getEpochSecond() + 12 * 3600);
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(P256KeyCodec.fromBase64Url(parts[2])).hasSize(64);
        assertThat(verifier.verify(P256KeyCodec.fromBase64Url(parts[2]))).isTrue();
    }

    /** Odszyfrowanie po stronie przegladarki wg RFC 8291 - niezalezna implementacja do sprawdzenia szyfrowania. */
    private static byte[] decryptAsUserAgent(byte[] body, ECPrivateKey uaPrivate, byte[] uaPublic, byte[] authSecret) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(body);
        byte[] salt = new byte[16];
        buffer.get(salt);
        assertThat(buffer.getInt()).isEqualTo(4096);
        byte[] serverPublic = new byte[buffer.get()];
        buffer.get(serverPublic);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(uaPrivate);
        agreement.doPhase(P256KeyCodec.decodePublicKey(serverPublic), true);
        byte[] keyInfo = ByteBuffer.allocate(14 + 65 + 65).put("WebPush: info\0".getBytes(StandardCharsets.US_ASCII))
                .put(uaPublic).put(serverPublic).array();
        byte[] ikm = WebPushPayloadEncryptor.hkdf(authSecret, agreement.generateSecret(), keyInfo, 32);
        byte[] cek = WebPushPayloadEncryptor.hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = WebPushPayloadEncryptor.hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);
        assertThat(padded[padded.length - 1]).isEqualTo((byte) 0x02);
        return Arrays.copyOf(padded, padded.length - 1);
    }
}
