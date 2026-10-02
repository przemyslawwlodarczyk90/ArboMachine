package com.example.arbomachine.domain.dataexchange.export;

import java.math.BigDecimal;
import java.util.List;

/**
 * Grupa drzew w eksporcie (etap 29): nazwa, uwagi, zrodlo, pole, pierscienie wielokata w WGS 84 ([dlugosc, szerokosc],
 * pierwszy = obrys) i numery przypisanych drzew.
 */
public record TreeStandExportData(
        long standId,
        String name,
        String notes,
        String source,
        BigDecimal areaSquareMeters,
        List<List<double[]>> rings,
        List<String> treeInventoryNumbers
) {

    public static final String TREE_NUMBERS_SEPARATOR = ", ";

    public String treeNumbersText() {
        return treeInventoryNumbers.isEmpty() ? null : String.join(TREE_NUMBERS_SEPARATOR, treeInventoryNumbers);
    }
}
