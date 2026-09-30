package com.example.arbomachine.domain.treephoto.dto;

import org.springframework.core.io.Resource;

/** Tresc zdjecia do wyslania klientowi: plik ze storage + metadane potrzebne w naglowkach odpowiedzi. */
public record TreePhotoContent(
        Resource content,
        String contentType,
        String originalFileName,
        long fileSizeBytes
) {}
