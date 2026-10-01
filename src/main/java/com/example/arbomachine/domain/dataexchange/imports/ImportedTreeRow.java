package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.util.List;
import java.util.Map;

/**
 * Wiersz pliku po odczycie, przed walidacja: komorki rozpoznanych pol importowalnych i problemy wykryte juz przy
 * odczycie (np. geometria GeoJSON inna niz Point). Format pliku nie ma tu znaczenia.
 */
public record ImportedTreeRow(int rowNumber, Map<TreeExchangeField, ImportedCell> cells, List<TreeImportIssue> readingIssues) {

    public ImportedCell cell(TreeExchangeField field) {
        ImportedCell cell = cells.get(field);
        return cell != null ? cell : new ImportedCell(field, field.polishLabel(), null);
    }

    public boolean isEmpty() {
        return readingIssues.isEmpty() && cells.values().stream().allMatch(ImportedCell::isBlank);
    }
}
