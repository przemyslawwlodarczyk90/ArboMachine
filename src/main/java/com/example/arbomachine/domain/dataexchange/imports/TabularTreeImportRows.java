package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.FieldInventoryColumnAliases;
import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Wspolna logika plikow tabelarycznych (CSV, XLSX): pierwszy wiersz = naglowki (polskie etykiety, nazwy techniczne
 * albo nazwy GIS - TreeExchangeField.fromHeader), kolejne = dane. Kolumny nieznane i tylko eksportowe sa pomijane
 * (zwracane do pokazania uzytkownikowi), puste wiersze - pomijane. Brak kolumny numeru drzewa = blad calego pliku.
 * <p>
 * Etap 29 - arkusze z aplikacji terenowych (FieldInventoryColumnAliases): dodatkowe nazwy kolumn, kolumny karty oceny
 * drzewa laczone w "Stan drzewa", wiersze obszarow pomijane, obwody wielopniowe przenoszone do notatek (z ostrzezeniem -
 * aplikacja nie przelicza obwodu wielopniowego).
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
        List<Integer> conditionCardColumns = new ArrayList<>();
        Integer areaColumn = null;
        Integer multiStemSumColumn = null;
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            String header = headers.get(columnIndex) == null ? "" : headers.get(columnIndex).trim();
            if (header.isEmpty()) continue;
            Optional<TreeExchangeField> field = TreeExchangeField.fromHeader(header).or(() -> FieldInventoryColumnAliases.fieldOf(header));
            if (field.isPresent() && field.get().importable() && recognizedFields.add(field.get())) {
                fieldByColumnIndex.put(columnIndex, field.get());
            } else if (FieldInventoryColumnAliases.isConditionCardColumn(header)) {
                conditionCardColumns.add(columnIndex);
            } else if (FieldInventoryColumnAliases.isAreaColumn(header)) {
                areaColumn = columnIndex;
            } else if (FieldInventoryColumnAliases.isMultiStemSumColumn(header)) {
                multiStemSumColumn = columnIndex;
            } else {
                ignoredColumns.add(header);
            }
        }
        if (!recognizedFields.contains(TreeExchangeField.INVENTORY_NUMBER)) {
            throw new TreeImportFileException(TreeImportIssueCode.MISSING_REQUIRED_COLUMN,
                    TreeExchangeField.INVENTORY_NUMBER.polishLabel());
        }
        if (!conditionCardColumns.isEmpty()) recognizedFields.add(TreeExchangeField.CONDITION_NOTES);

        List<ImportedTreeRow> importedRows = new ArrayList<>();
        List<TreeImportIssue> warnings = new ArrayList<>();
        for (NumberedRow row : nonEmptyRows.subList(1, nonEmptyRows.size())) {
            if (areaColumn != null && hasText(row.valueAt(areaColumn))) {
                warnings.add(new TreeImportIssue(row.rowNumber(), null, headers.get(areaColumn).trim(), row.valueAt(areaColumn),
                        TreeImportIssueCode.AREA_ROW_SKIPPED));
                continue;
            }
            Map<TreeExchangeField, ImportedCell> cells = new EnumMap<>(TreeExchangeField.class);
            fieldByColumnIndex.forEach((columnIndex, field) -> cells.put(field,
                    new ImportedCell(field, headers.get(columnIndex).trim(), row.valueAt(columnIndex))));
            mergeConditionCard(row, headers, conditionCardColumns, cells);
            moveMultiStemCircumferenceToNotes(row, headers, multiStemSumColumn, cells, recognizedFields, warnings);
            importedRows.add(new ImportedTreeRow(row.rowNumber(), cells, List.of()));
        }
        return new ParsedTreeImportFile(recognizedFields, ignoredColumns, importedRows, warnings);
    }

    /** Kolumny karty oceny -> "Stan drzewa": "Pień: pochylony; Gałęzie - posusz: 5%". Istniejacy tekst stanu zostaje na poczatku. */
    private static void mergeConditionCard(NumberedRow row, List<String> headers, List<Integer> conditionCardColumns,
                                           Map<TreeExchangeField, ImportedCell> cells) {
        if (conditionCardColumns.isEmpty()) return;
        List<String> parts = new ArrayList<>();
        ImportedCell existing = cells.get(TreeExchangeField.CONDITION_NOTES);
        if (existing != null && !existing.isBlank()) parts.add(existing.rawValue().trim());
        for (int columnIndex : conditionCardColumns) {
            String value = row.valueAt(columnIndex);
            if (hasText(value)) parts.add(headers.get(columnIndex).trim() + ": " + value.trim());
        }
        cells.put(TreeExchangeField.CONDITION_NOTES, new ImportedCell(TreeExchangeField.CONDITION_NOTES,
                existing != null ? existing.columnName() : "Karta drzewa", parts.isEmpty() ? null : String.join("; ", parts)));
    }

    private static void moveMultiStemCircumferenceToNotes(NumberedRow row, List<String> headers, Integer multiStemSumColumn,
                                                          Map<TreeExchangeField, ImportedCell> cells,
                                                          Set<TreeExchangeField> recognizedFields, List<TreeImportIssue> warnings) {
        ImportedCell circumference = cells.get(TreeExchangeField.TRUNK_CIRCUMFERENCE_CM);
        if (circumference == null || !FieldInventoryColumnAliases.isMultiStemCircumference(circumference.rawValue())) return;
        String sumInFile = multiStemSumColumn == null ? null : row.valueAt(multiStemSumColumn);
        String note = "Obwody pni na 130 cm: " + circumference.rawValue().trim()
                + (hasText(sumInFile) ? " (suma podana w pliku: " + sumInFile.trim() + ")" : "");
        ImportedCell notes = cells.get(TreeExchangeField.NOTES);
        String combinedNotes = notes == null || notes.isBlank() ? note : notes.rawValue().trim() + "; " + note;
        cells.put(TreeExchangeField.NOTES, new ImportedCell(TreeExchangeField.NOTES,
                notes != null ? notes.columnName() : TreeExchangeField.NOTES.polishLabel(), combinedNotes));
        recognizedFields.add(TreeExchangeField.NOTES);
        warnings.add(TreeImportIssue.ofCell(row.rowNumber(), circumference, TreeImportIssueCode.MULTI_STEM_CIRCUMFERENCE_IN_NOTES));
        cells.put(TreeExchangeField.TRUNK_CIRCUMFERENCE_CM, new ImportedCell(TreeExchangeField.TRUNK_CIRCUMFERENCE_CM,
                circumference.columnName(), null));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
