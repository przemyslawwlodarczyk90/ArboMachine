package com.example.arbomachine.domain.notification.push;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Naglowek Authorization dla uslugi push - VAPID (RFC 8292): JWT ES256 z aud = origin endpointu, exp (12 h)
 * i sub (kontakt do administratora serwera) + klucz publiczny serwera.
 */
@Component
public class VapidAuthorizationFactory {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(12);

    private final ObjectMapper objectMapper;

    public VapidAuthorizationFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String authorizationHeaderFor(URI pushEndpoint, WebPushServerKeys serverKeys, String subject, Instant now) {
        String audience = pushEndpoint.getScheme() + "://" + pushEndpoint.getRawAuthority();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("aud", audience);
        claims.put("exp", now.plus(TOKEN_VALIDITY).getEpochSecond());
        claims.put("sub", subject);
        try {
            String signingInput = P256KeyCodec.base64Url("{\"typ\":\"JWT\",\"alg\":\"ES256\"}".getBytes(StandardCharsets.UTF_8))
                    + "." + P256KeyCodec.base64Url(objectMapper.writeValueAsBytes(claims));
            // JWS ES256 wymaga podpisu R || S (64 bajty), nie DER - stad format P1363.
            Signature signature = Signature.getInstance("SHA256withECDSAinP1363Format");
            signature.initSign(serverKeys.privateKey());
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            String token = signingInput + "." + P256KeyCodec.base64Url(signature.sign());
            return "vapid t=" + token + ", k=" + serverKeys.publicKeyBase64Url();
        } catch (GeneralSecurityException | JsonProcessingException signingError) {
            throw new IllegalStateException("VAPID signing failed", signingError);
        }
    }
}
