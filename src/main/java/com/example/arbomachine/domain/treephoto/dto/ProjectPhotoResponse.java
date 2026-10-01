package com.example.arbomachine.domain.treephoto.dto;

import java.time.Instant;

/**
 * Zdjecie na liscie materialow do dokumentacji projektu (GET /api/projects/{projectId}/photos) - z numerem drzewa
 * i informacja o adnotacji, bez sciezek i kluczy plikow.
 */
public record ProjectPhotoResponse(
        Long photoId,
        Long treeId,
        String inventoryNumber,
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        boolean hasAnnotation,
        Instant annotationUpdatedAt,
        Instant createdAt
) {}
