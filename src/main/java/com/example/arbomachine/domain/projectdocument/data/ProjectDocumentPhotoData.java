package com.example.arbomachine.domain.projectdocument.data;

/**
 * Zdjecie w dokumencie. documentPhotoNumber - kolejny numer "Fot. N" w calym dokumencie. annotated - uzyto
 * splaszczonej wersji z adnotacja (etap 20). embeddable = false, gdy formatu nie da sie osadzic w PDF (V1: WebP
 * bez adnotacji) - wtedy dokument pokazuje tylko podpis. Tresc jest czytana leniwie, dopiero przy renderowaniu.
 * Etap 52: caption - podpis arborysty (null = podpis z nazwy pliku); zdjecia wylaczone z dokumentu tu nie trafiaja.
 */
public record ProjectDocumentPhotoData(
        int documentPhotoNumber,
        String originalFileName,
        boolean annotated,
        boolean embeddable,
        DocumentImageContent content,
        String caption
) {
    /** Tekst po "Fot. N. Drzewo nr X — ": podpis arborysty albo nazwa pliku. */
    public String captionOrFileName() {
        return caption != null ? caption : originalFileName;
    }
}
