package com.example.arbomachine.infrastructure.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Wystawianie i weryfikacja tokenow JWT (HMAC256). Temat tokenu (subject) = e-mail uzytkownika. */
@Component
public class JwtTokenService {

    @Value("${auth.jwt.secret}")
    private String signingSecret;

    @Value("${auth.jwt.token-validity-days}")
    private long tokenValidityDays;

    @Value("${auth.jwt.issuer}")
    private String tokenIssuer;

    public String createAccessTokenForEmail(String email) {
        // Instant.now() jest zawsze w UTC - wczesniej LocalDateTime traktowany jako UTC dawal w Polsce
        // czas wystawienia 2h w przyszlosci i weryfikacja odrzucala swiezo wydane tokeny.
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofDays(tokenValidityDays));

        return JWT.create()
                .withSubject(email)
                .withIssuer(tokenIssuer)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(expiresAt))
                .sign(signingAlgorithm());
    }

    /** Zwraca e-mail z tokenu; rzuca wyjatek, jesli podpis, wystawca albo data waznosci sie nie zgadzaja. */
    public String extractEmailFromToken(String token) {
        return tokenVerifier().verify(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            tokenVerifier().verify(token);
            return true;
        } catch (Exception invalidTokenException) {
            return false;
        }
    }

    private JWTVerifier tokenVerifier() {
        return JWT.require(signingAlgorithm())
                .withIssuer(tokenIssuer)
                .build();
    }

    private Algorithm signingAlgorithm() {
        return Algorithm.HMAC256(signingSecret);
    }
}
