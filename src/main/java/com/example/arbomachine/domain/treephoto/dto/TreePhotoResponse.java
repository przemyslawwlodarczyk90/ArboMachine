package com.example.arbomachine.domain.treephoto.dto;

import java.time.Instant;

/**
 * Metadane zdjecia zwracane przez API. Celowo BEZ klucza storage i sciezki pliku - tresc pobiera sie
 * endpointem .../photos/{photoId}/content.
 */
public record TreePhotoResponse(
        Long id,
        Long treeId,
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        /** czy zdjecie ma aktualna adnotacje (szkic) - tresc do dokumentacji: .../documentation-content */
        boolean hasAnnotation,
        Instant annotationUpdatedAt,
        Instant createdAt
) {}
