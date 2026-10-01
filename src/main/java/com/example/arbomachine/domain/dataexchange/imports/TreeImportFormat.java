package com.example.arbomachine.domain.dataexchange.imports;

import java.util.Locale;
import java.util.Optional;

/** Formaty importu drzew V1 - rozpoznawane po rozszerzeniu nazwy pliku. */
public enum TreeImportFormat {
    CSV, XLSX, GEOJSON;

    public static Optional<TreeImportFormat> fromFileName(String fileName) {
        if (fileName == null) return Optional.empty();
        String lowerCaseName = fileName.trim().toLowerCase(Locale.ROOT);
        if (lowerCaseName.endsWith(".csv")) return Optional.of(CSV);
        if (lowerCaseName.endsWith(".xlsx")) return Optional.of(XLSX);
        if (lowerCaseName.endsWith(".geojson") || lowerCaseName.endsWith(".json")) return Optional.of(GEOJSON);
        return Optional.empty();
    }
}
