package com.example.arbomachine.domain.treephoto.dto;

import java.time.Instant;
import java.util.List;

/** Aktualna adnotacja zdjecia - bez klucza storage splaszczonego PNG (tresc: .../annotation/content). */
public record PhotoAnnotationResponse(
        Long photoId,
        int schemaVersion,
        List<PhotoAnnotationElementDto> elements,
        Instant createdAt,
        Instant updatedAt
) {}
