package com.example.arbomachine.domain.treephoto.dto;

import java.util.List;

/**
 * Dokument adnotacji wysylany jako czesc "document" (application/json) requestu multipart PUT .../annotation.
 * Walidowany w calosci przed zapisem (PhotoAnnotationDocumentValidator) - do bazy trafia dokument po walidacji,
 * nigdy surowy JSON od klienta. Bez identyfikatorow projektu/drzewa/zdjecia - pochodza z URL.
 */
public record PhotoAnnotationDocumentRequest(
        Integer schemaVersion,
        List<PhotoAnnotationElementDto> elements
) {}
