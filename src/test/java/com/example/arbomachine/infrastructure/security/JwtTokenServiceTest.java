package com.example.arbomachine.infrastructure.security;

import com.auth0.jwt.JWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    private JwtTokenService createServiceSignedWith(String signingSecret) {
        JwtTokenService service = new JwtTokenService();
        ReflectionTestUtils.setField(service, "signingSecret", signingSecret);
        ReflectionTestUtils.setField(service, "tokenValidityDays", 7L);
        ReflectionTestUtils.setField(service, "tokenIssuer", "ArboMachine");
        return service;
    }

    @BeforeEach
    void createService() {
        jwtTokenService = createServiceSignedWith("test-signing-secret");
    }

    @Test
    void freshTokenIsValidAndCarriesEmail() {
        String accessToken = jwtTokenService.createAccessTokenForEmail("jan@example.com");

        assertTrue(jwtTokenService.isTokenValid(accessToken));
        assertEquals("jan@example.com", jwtTokenService.extractEmailFromToken(accessToken));
    }

    /** Regresja: czas wystawienia liczony z LocalDateTime w strefie +02:00 wypadal w przyszlosci. */
    @Test
    void issuedAtIsNotInTheFuture() {
        String accessToken = jwtTokenService.createAccessTokenForEmail("jan@example.com");

        Instant issuedAt = JWT.decode(accessToken).getIssuedAtAsInstant();
        assertFalse(issuedAt.isAfter(Instant.now().plusSeconds(1)));
    }

    @Test
    void tokenSignedWithDifferentSecretIsRejected() {
        String foreignToken = createServiceSignedWith("other-secret").createAccessTokenForEmail("jan@example.com");

        assertFalse(jwtTokenService.isTokenValid(foreignToken));
    }

    @Test
    void garbageIsRejected() {
        assertFalse(jwtTokenService.isTokenValid("not-a-jwt"));
    }
}
