package com.example.arbomachine.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Tylko dla profilu "test": przepuszcza wszystkie zadania, zeby testy kontrolerow sprawdzaly logike,
 * a nie reguly dostepu. Lancuch filtrow z SecurityConfig jest w tym profilu wylaczony (@Profile("!test")).
 * Reguly dostepu sprawdza osobno SecurityRulesTest - bez tego profilu.
 */
@Configuration
@Profile("test")
public class PermitAllTestSecurityConfig {

    @Bean
    public SecurityFilterChain permitAllSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorization -> authorization.anyRequest().permitAll())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
