package com.example.arbomachine.domain.dataexchange.dto;

import com.example.arbomachine.domain.dataexchange.imports.TreeImportIssue;

import java.util.List;

/**
 * Wynik sprawdzenia pliku importu (i odpowiedz 400 odrzuconego importu). readyToImport = brak bledow pliku i wierszy
 * oraz co najmniej jeden wiersz - import V1 jest "wszystko albo nic". warnings nie blokuja importu (np. duplikat numeru).
 * format - CSV / XLSX / GEOJSON albo null, gdy formatu nie rozpoznano.
 */
public record TreeImportValidationResponse(
        String fileName,
        String format,
        int totalRowCount,
        int validRowCount,
        int invalidRowCount,
        boolean readyToImport,
        List<TreeImportIssue> fileErrors,
        List<TreeImportIssue> errors,
        List<TreeImportIssue> warnings,
        List<String> ignoredColumns,
        List<TreeImportPreviewRowResponse> preview
) {}
