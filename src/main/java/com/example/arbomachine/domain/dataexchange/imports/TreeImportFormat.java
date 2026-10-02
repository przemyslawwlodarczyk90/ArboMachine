package com.example.arbomachine.domain.dataexchange.imports;

import java.util.Locale;
import java.util.Optional;

/** Formaty importu drzew i grup drzew - rozpoznawane po rozszerzeniu nazwy pliku. */
public enum TreeImportFormat {
    CSV, XLSX, GEOJSON,
    /** etap 29: Shapefile spakowany do ZIP (.shp + .dbf, opcjonalnie .prj / .cpg) */
    SHAPEFILE,
    GEOPACKAGE,
    /** etap 29: rysunek CAD (ASCII DXF) */
    DXF;

    public static Optional<TreeImportFormat> fromFileName(String fileName) {
        if (fileName == null) return Optional.empty();
        String lowerCaseName = fileName.trim().toLowerCase(Locale.ROOT);
        if (lowerCaseName.endsWith(".csv")) return Optional.of(CSV);
        if (lowerCaseName.endsWith(".xlsx")) return Optional.of(XLSX);
        if (lowerCaseName.endsWith(".geojson") || lowerCaseName.endsWith(".json")) return Optional.of(GEOJSON);
        if (lowerCaseName.endsWith(".zip")) return Optional.of(SHAPEFILE);
        if (lowerCaseName.endsWith(".gpkg")) return Optional.of(GEOPACKAGE);
        if (lowerCaseName.endsWith(".dxf")) return Optional.of(DXF);
        return Optional.empty();
    }
}
