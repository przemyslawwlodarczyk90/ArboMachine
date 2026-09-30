package com.example.arbomachine.infrastructure.storage;

import com.example.arbomachine.domain.treephoto.config.TreePhotoProperties;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Limit multipart serwera wyliczony z app.tree-photos.max-file-size - limit zdjecia ustawia sie w jednym miejscu.
 * (Wlasny MultipartConfigElement zastepuje spring.servlet.multipart.* z autokonfiguracji Spring Boot.)
 * Bez tego Spring odrzucalby juz pliki powyzej domyslnego 1 MB.
 */
@Configuration
@EnableConfigurationProperties(TreePhotoProperties.class)
public class TreePhotoUploadConfig {

    /** Zapas na pozostale czesci zadania multipart (naglowki czesci, ewentualne pola tekstowe). */
    private static final DataSize MULTIPART_REQUEST_OVERHEAD = DataSize.ofMegabytes(1);

    @Bean
    public MultipartConfigElement multipartConfigElement(TreePhotoProperties treePhotoProperties) {
        long maximumFileSizeBytes = treePhotoProperties.maxFileSize().toBytes();
        return new MultipartConfigElement("", maximumFileSizeBytes,
                maximumFileSizeBytes + MULTIPART_REQUEST_OVERHEAD.toBytes(), 0);
    }
}
