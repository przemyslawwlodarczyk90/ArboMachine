package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wspolna logika plikow tabelarycznych (CSV, XLSX): pierwszy wiersz = naglowki (polskie etykiety, nazwy techniczne
 * albo nazwy GIS - TreeExchangeField.fromHeader), kolejne = dane. Kolumny nieznane i tylko eksportowe sa pomijane
 * (zwracane do pokazania uzytkownikowi), puste wiersze - pomijane. Brak kolumny numeru drzewa = blad calego pliku.
 */
final class TabularTreeImportRows {

    private TabularTreeImportRows() {
    }

    /** rows - wiersze pliku z numerem wiersza arkusza (naglowek = pierwszy niepusty wiersz). */
    static ParsedTreeImportFile parse(List<NumberedRow> rows) {
        List<NumberedRow> nonEmptyRows = rows.stream().filter(row -> !row.isBlank()).toList();
        if (nonEmptyRows.isEmpty()) throw new TreeImportFileException(TreeImportIssueCode.EMPTY_FILE);
        List<String> headers = nonEmptyRows.getFirst().values();

        Map<Integer, TreeExchangeField> fieldByColumnIndex = new java.util.HashMap<>();
        Set<TreeExchangeField> recognizedFields = new LinkedHashSet<>();
        List<String> ignoredColumns = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            String header = headers.get(columnIndex) == null ? "" : headers.get(columnIndex).trim();
            if (header.isEmpty()) continue;
            var field = TreeExchangeField.fromHeader(header);
            if (field.isPresent() && field.get().importable() && recognizedFields.add(field.get())) {
                fieldByColumnIndex.put(columnIndex, field.get());
            } else {
                ignoredColumns.add(header);
            }
        }
        if (!recognizedFields.contains(TreeExchangeField.INVENTORY_NUMBER)) {
            throw new TreeImportFileException(TreeImportIssueCode.MISSING_REQUIRED_COLUMN,
                    TreeExchangeField.INVENTORY_NUMBER.polishLabel());
        }

        List<ImportedTreeRow> importedRows = new ArrayList<>();
        for (NumberedRow row : nonEmptyRows.subList(1, nonEmptyRows.size())) {
            Map<TreeExchangeField, ImportedCell> cells = new EnumMap<>(TreeExchangeField.class);
            fieldByColumnIndex.forEach((columnIndex, field) -> cells.put(field,
                    new ImportedCell(field, headers.get(columnIndex).trim(), row.valueAt(columnIndex))));
            importedRows.add(new ImportedTreeRow(row.rowNumber(), cells, List.of()));
        }
        return new ParsedTreeImportFile(recognizedFields, ignoredColumns, importedRows);
    }

    record NumberedRow(int rowNumber, List<String> values) {

        boolean isBlank() {
            return values.stream().allMatch(value -> value == null || value.isBlank());
        }

        String valueAt(int columnIndex) {
            return columnIndex < values.size() ? values.get(columnIndex) : null;
        }
    }
}
