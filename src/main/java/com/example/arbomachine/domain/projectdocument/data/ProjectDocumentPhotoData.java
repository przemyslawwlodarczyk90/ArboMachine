package com.example.arbomachine.domain.projectdocument.data;

/**
 * Zdjecie w dokumencie. documentPhotoNumber - kolejny numer "Fot. N" w calym dokumencie. annotated - uzyto
 * splaszczonej wersji z adnotacja (etap 20). embeddable = false, gdy formatu nie da sie osadzic w PDF (V1: WebP
 * bez adnotacji) - wtedy dokument pokazuje tylko podpis. Tresc jest czytana leniwie, dopiero przy renderowaniu.
 */
public record ProjectDocumentPhotoData(
        int documentPhotoNumber,
        String originalFileName,
        boolean annotated,
        boolean embeddable,
        DocumentImageContent content
) {}
