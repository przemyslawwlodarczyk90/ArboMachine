package com.example.arbomachine.infrastructure.api;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serwowanie zbudowanego frontendu (React) z classpath:/static - "npm run build" w folderze frontend
 * zapisuje tam index.html i assets/. Trasy Reacta (np. /projects/5/trees/7) nie istnieja jako pliki,
 * wiec kazda sciezka bez pliku dostaje index.html i routing przejmuje przegladarka (odswiezenie, deep-link).
 * Wyjatki: /api/**, Swagger i actuator - tam brak zasobu zostaje zwyklym 404, a nie strona aplikacji.
 * Bez zbudowanego frontendu "/" przekierowuje do Swagger UI (jak dotychczas).
 */
@Configuration
public class FrontendSinglePageAppConfig implements WebMvcConfigurer {

    private static final String FRONTEND_LOCATION = "classpath:/static/";
    private static final ClassPathResource FRONTEND_INDEX_PAGE = new ClassPathResource("static/index.html");

    @Override
    public void addViewControllers(ViewControllerRegistry viewControllerRegistry) {
        if (FRONTEND_INDEX_PAGE.exists()) {
            viewControllerRegistry.addViewController("/").setViewName("forward:/index.html");
        } else {
            viewControllerRegistry.addRedirectViewController("/", "/swagger-ui.html");
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {
        resourceHandlerRegistry.addResourceHandler("/**")
                .addResourceLocations(FRONTEND_LOCATION)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        if (isBackendPath(resourcePath)) {
                            return null;
                        }
                        return FRONTEND_INDEX_PAGE.exists() ? FRONTEND_INDEX_PAGE : null;
                    }
                });
    }

    private static boolean isBackendPath(String resourcePath) {
        return resourcePath.startsWith("api/") || resourcePath.startsWith("swagger-ui")
                || resourcePath.startsWith("v3/") || resourcePath.startsWith("actuator");
    }
}
