package com.example.arbomachine.domain.dataexchange.dto;

import com.example.arbomachine.domain.dataexchange.imports.TreeImportIssue;

import java.math.BigDecimal;
import java.util.List;

/**
 * Wynik "Sprawdz plik" importu grup drzew (etap 29): obszary gotowe do importu, bledy obiektow (numer obiektu w pliku),
 * ostrzezenia (np. przeliczenie ukladu) i podglad - nazwa, pole, liczba drzew projektu w obszarze.
 */
public record TreeStandImportValidationResponse(
        String fileName,
        String format,
        int featureCount,
        int validStandCount,
        int invalidFeatureCount,
        boolean readyToImport,
        List<TreeImportIssue> fileErrors,
        List<TreeImportIssue> errors,
        List<TreeImportIssue> warnings,
        List<PreviewStand> preview
) {

    public record PreviewStand(int rowNumber, String name, BigDecimal areaSquareMeters, int vertexCount, int treeCountInside) {}
}
