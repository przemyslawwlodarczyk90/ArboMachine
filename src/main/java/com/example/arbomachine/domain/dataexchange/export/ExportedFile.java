package com.example.arbomachine.domain.dataexchange.export;

/** Gotowy plik eksportu: tresc, typ MIME, nazwa pliku (tylko id projektu - bez danych wpisanych przez uzytkownika). */
public record ExportedFile(byte[] content, String contentType, String fileName) {

    public static String fileNameOf(ProjectExportData data, String extension) {
        return "arbomachine-projekt-" + data.projectId() + "-drzewa." + extension;
    }
}
