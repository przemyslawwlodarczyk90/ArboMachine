package com.example.arbomachine.domain.notification.push;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

/**
 * Szyfrowanie tresci powiadomienia Web Push - RFC 8291 ("aes128gcm", RFC 8188), jeden rekord bez dopelnienia.
 * Dla kazdej wiadomosci nowa para kluczy serwera (ECDH) i losowa sol. Sprawdzone wektorem z RFC 8291, dodatek A.
 */
@Component
public class WebPushPayloadEncryptor {

    /** Rozmiar rekordu z przykladu RFC 8291; tresc powiadomien ArboMachine jest wielokrotnie mniejsza. */
    static final int RECORD_SIZE = 4096;
    /** Bezpieczny limit tresci (uslugi push przyjmuja ok. 4 kB po zaszyfrowaniu). */
    public static final int MAXIMUM_PLAINTEXT_LENGTH = 3000;
    private static final int SALT_LENGTH = 16;
    private static final int AUTH_SECRET_LENGTH = 16;
    private static final byte PADDING_DELIMITER = 0x02;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * @param userAgentPublicKey klucz p256dh subskrypcji (65 bajtow)
     * @param authSecret         sekret auth subskrypcji (16 bajtow)
     * @return gotowe cialo zadania (naglowek RFC 8188 + szyfrogram)
     */
    public byte[] encrypt(byte[] plaintext, byte[] userAgentPublicKey, byte[] authSecret) {
        if (plaintext.length > MAXIMUM_PLAINTEXT_LENGTH) throw new IllegalArgumentException("Push payload too large");
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return encrypt(plaintext, userAgentPublicKey, authSecret, P256KeyCodec.generateKeyPair(), salt);
    }

    /** Wersja deterministyczna (klucze serwera i sol podane) - do testu wektorem z RFC. */
    byte[] encrypt(byte[] plaintext, byte[] userAgentPublicKey, byte[] authSecret, KeyPair serverKeyPair, byte[] salt) {
        if (authSecret.length != AUTH_SECRET_LENGTH) throw new IllegalArgumentException("Auth secret must have 16 bytes");
        try {
            ECPublicKey userAgentKey = P256KeyCodec.decodePublicKey(userAgentPublicKey);
            byte[] serverPublicKey = P256KeyCodec.encodePublicKey((ECPublicKey) serverKeyPair.getPublic());

            KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init((ECPrivateKey) serverKeyPair.getPrivate());
            keyAgreement.doPhase(userAgentKey, true);
            byte[] ecdhSecret = keyAgreement.generateSecret();

            byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), userAgentPublicKey, serverPublicKey);
            byte[] inputKeyingMaterial = hkdf(authSecret, ecdhSecret, keyInfo, 32);
            byte[] contentEncryptionKey = hkdf(salt, inputKeyingMaterial,
                    "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
            byte[] nonce = hkdf(salt, inputKeyingMaterial, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(contentEncryptionKey, "AES"), new GCMParameterSpec(128, nonce));
            byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[]{PADDING_DELIMITER}));

            ByteBuffer header = ByteBuffer.allocate(SALT_LENGTH + 4 + 1 + serverPublicKey.length);
            header.put(salt).putInt(RECORD_SIZE).put((byte) serverPublicKey.length).put(serverPublicKey);
            return concat(header.array(), ciphertext);
        } catch (GeneralSecurityException cryptoError) {
            throw new IllegalStateException("Web Push encryption failed", cryptoError);
        }
    }

    /** HKDF-SHA-256 (RFC 5869) dla wynikow do 32 bajtow - jeden blok expand. */
    static byte[] hkdf(byte[] salt, byte[] inputKeyingMaterial, byte[] info, int length) throws GeneralSecurityException {
        byte[] pseudoRandomKey = hmacSha256(salt, inputKeyingMaterial);
        return Arrays.copyOf(hmacSha256(pseudoRandomKey, concat(info, new byte[]{0x01})), length);
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        for (byte[] part : parts) joined.writeBytes(part);
        return joined.toByteArray();
    }
}
