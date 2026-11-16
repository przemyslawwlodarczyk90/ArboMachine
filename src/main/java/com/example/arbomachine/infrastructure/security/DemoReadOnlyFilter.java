package com.example.arbomachine.infrastructure.security;

import com.example.arbomachine.domain.demo.DemoAccountPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Tryb demo jest tylko do odczytu - egzekwowane tutaj, niezaleznie od wygladu przyciskow we frontendzie.
 * <p>
 * Kazde zadanie do /api/** z konta demo metoda inna niz GET/HEAD/OPTIONS dostaje 403 z kodem DEMO_READ_ONLY,
 * zanim dotrze do kontrolera. Dotyczy to wszystkich modulow (projekty, drzewa, zdjecia, import, profil, usuniecie
 * konta, Web Push) - takze tych dodanych w przyszlosci, bo filtr nie zna listy endpointow.
 * <p>
 * Celowo NIE jest beanem Springa (wtedy Spring Boot zarejestrowalby go drugi raz jako zwykly filtr serwletow);
 * tworzy go SecurityConfig i wstawia za JwtAuthenticationFilter.
 */
public class DemoReadOnlyFilter extends OncePerRequestFilter {

    public static final String DEMO_READ_ONLY_MESSAGE = "Demo mode is read-only";
    private static final Set<String> READ_ONLY_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final DemoAccountPolicy demoAccountPolicy;

    public DemoReadOnlyFilter(DemoAccountPolicy demoAccountPolicy) {
        this.demoAccountPolicy = demoAccountPolicy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isWriteAttemptFromDemoAccount(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"code\":\"DEMO_READ_ONLY\",\"message\":\""
                    + DEMO_READ_ONLY_MESSAGE + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isWriteAttemptFromDemoAccount(HttpServletRequest request) {
        if (READ_ONLY_METHODS.contains(request.getMethod().toUpperCase())) return false;
        if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/")) return false;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && demoAccountPolicy.isDemoAccountEmail(authentication.getName());
    }
}
