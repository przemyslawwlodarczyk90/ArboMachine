package com.example.arbomachine.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Reguly dostepu:
 * <ul>
 *   <li>rejestracja, logowanie, potwierdzenie e-maila i reset hasla - publiczne,</li>
 *   <li>/api/admin/** - tylko ADMIN (jedyne miejsce, w ktorym mozna ruszac cudze konta),</li>
 *   <li>/api/arborist/** - tylko ARBORIST, /api/official/** - tylko OFFICIAL,</li>
 *   <li>/api/users/** (wlasne konto) - kazdy zalogowany,</li>
 *   <li>/api/projects/** - kazdy zalogowany; wlasnosc projektu pilnuje FindProjectOwnedByCurrentUserHandler,</li>
 *   <li>/api/notifications/** (wlasne powiadomienia i Web Push) - kazdy zalogowany.</li>
 * </ul>
 * Brak tokenu = 401, zla rola = 403.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /** W profilu "test" zastepuje go PermitAllTestSecurityConfig. */
    @Bean
    @Profile("!test")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorization -> authorization
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/login",
                                "/api/users/confirm",
                                "/api/users/password-reset/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/arborist/**").hasRole("ARBORIST")
                        .requestMatchers("/api/official/**").hasRole("OFFICIAL")
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/projects", "/api/projects/**").authenticated()
                        .requestMatchers("/api/map-symbols").authenticated()
                        .requestMatchers("/api/notifications", "/api/notifications/**").authenticated()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authenticationException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
