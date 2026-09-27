package com.example.arbomachine.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Czyta naglowek "Authorization: Bearer &lt;token&gt;" i, jesli token jest wazny, loguje uzytkownika
 * na czas tego jednego zadania (aplikacja jest bezstanowa - bez sesji).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserDetailsService userDetailsService) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");

        // Niewazny/wygasly token, usuniete, niepotwierdzone albo zablokowane konto = zadanie anonimowe;
        // chronione endpointy odpowiedza wtedy 401.
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());
            if (jwtTokenService.isTokenValid(accessToken)) {
                authenticateRequestIfAccountIsActive(request, accessToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateRequestIfAccountIsActive(HttpServletRequest request, String accessToken) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(jwtTokenService.extractEmailFromToken(accessToken));
            if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (UsernameNotFoundException accountDeletedAfterTokenWasIssued) {
            // konto usuniete po wystawieniu tokenu - zadanie zostaje anonimowe
        }
    }
}
