package com.example.arbomachine.infrastructure.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Backend to samo REST API - interfejs uzytkownika dziala we frontendzie (Vite, domyslnie http://localhost:5173).
 * Zeby wejscie na glowny adres backendu nie konczylo sie strona "Whitelabel Error Page" (404),
 * "/" przekierowuje do dokumentacji API w Swagger UI.
 */
@Configuration
public class RootPathRedirectConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry viewControllerRegistry) {
        viewControllerRegistry.addRedirectViewController("/", "/swagger-ui.html");
    }
}
