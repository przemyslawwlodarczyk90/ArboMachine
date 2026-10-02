package com.example.arbomachine.domain.notification.push;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Klucze krzywej P-256 w formacie Web Push: klucz publiczny jako nieskompresowany punkt (65 bajtow: 0x04 || X || Y),
 * prywatny jako 32-bajtowa liczba d; tekstowo base64url bez paddingu (jak w PushSubscription przegladarki).
 * Tylko kryptografia JDK (SunEC) - bez dodatkowych bibliotek.
 */
public final class P256KeyCodec {

    public static final int UNCOMPRESSED_POINT_LENGTH = 65;
    private static final int COORDINATE_LENGTH = 32;
    private static final ECParameterSpec P256 = p256Parameters();

    private P256KeyCodec() {
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            return generator.generateKeyPair();
        } catch (GeneralSecurityException cryptoError) {
            throw new IllegalStateException("P-256 is not available", cryptoError);
        }
    }

    public static byte[] encodePublicKey(ECPublicKey publicKey) {
        byte[] encoded = new byte[UNCOMPRESSED_POINT_LENGTH];
        encoded[0] = 0x04;
        System.arraycopy(unsignedFixedLength(publicKey.getW().getAffineX()), 0, encoded, 1, COORDINATE_LENGTH);
        System.arraycopy(unsignedFixedLength(publicKey.getW().getAffineY()), 0, encoded, 1 + COORDINATE_LENGTH, COORDINATE_LENGTH);
        return encoded;
    }

    public static byte[] encodePrivateKey(ECPrivateKey privateKey) {
        return unsignedFixedLength(privateKey.getS());
    }

    /** Rzuca IllegalArgumentException dla danych, ktore nie sa punktem P-256. */
    public static ECPublicKey decodePublicKey(byte[] uncompressedPoint) {
        if (uncompressedPoint == null || uncompressedPoint.length != UNCOMPRESSED_POINT_LENGTH || uncompressedPoint[0] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed P-256 point");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, 1, 1 + COORDINATE_LENGTH));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, 1 + COORDINATE_LENGTH, UNCOMPRESSED_POINT_LENGTH));
        if (!isOnCurve(x, y)) throw new IllegalArgumentException("Point is not on P-256");
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
        } catch (GeneralSecurityException cryptoError) {
            throw new IllegalArgumentException("Invalid P-256 public key", cryptoError);
        }
    }

    public static ECPrivateKey decodePrivateKey(byte[] privateScalar) {
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, privateScalar), P256));
        } catch (GeneralSecurityException cryptoError) {
            throw new IllegalArgumentException("Invalid P-256 private key", cryptoError);
        }
    }

    public static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Przyjmuje base64url z paddingiem albo bez (rozne przegladarki). */
    public static byte[] fromBase64Url(String text) {
        return Base64.getUrlDecoder().decode(text.trim().replace("=", ""));
    }

    /** y^2 = x^3 - 3x + b (mod p) - odrzuca punkty spoza krzywej przed ECDH. */
    private static boolean isOnCurve(BigInteger x, BigInteger y) {
        BigInteger p = ((java.security.spec.ECFieldFp) P256.getCurve().getField()).getP();
        if (x.signum() < 0 || x.compareTo(p) >= 0 || y.signum() < 0 || y.compareTo(p) >= 0) return false;
        BigInteger left = y.modPow(BigInteger.TWO, p);
        BigInteger right = x.modPow(BigInteger.valueOf(3), p).add(P256.getCurve().getA().multiply(x)).add(P256.getCurve().getB()).mod(p);
        return left.equals(right);
    }

    private static byte[] unsignedFixedLength(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes.length == COORDINATE_LENGTH) return bytes;
        byte[] fixed = new byte[COORDINATE_LENGTH];
        int copied = Math.min(bytes.length, COORDINATE_LENGTH);
        System.arraycopy(bytes, bytes.length - copied, fixed, COORDINATE_LENGTH - copied, copied);
        return fixed;
    }

    private static ECParameterSpec p256Parameters() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException cryptoError) {
            throw new IllegalStateException("P-256 is not available", cryptoError);
        }
    }
}
