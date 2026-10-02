package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.util.List;
import java.util.Set;

/**
 * Wynik odczytu pliku (dowolnego formatu): rozpoznane pola, kolumny pominiete (nieznane albo tylko eksportowe,
 * np. "ID drzewa (ArboMachine)"), niepuste wiersze danych i ostrzezenia dotyczace calego pliku (etap 29: przeliczenie
 * ukladu wspolrzednych, pominiete obszary, obwody wielopniowe w notatkach).
 */
public record ParsedTreeImportFile(Set<TreeExchangeField> recognizedFields, List<String> ignoredColumns,
                                   List<ImportedTreeRow> rows, List<TreeImportIssue> fileWarnings) {

    /** Bez ostrzezen pliku (CSV, XLSX, GeoJSON bez przeliczen). */
    public ParsedTreeImportFile(Set<TreeExchangeField> recognizedFields, List<String> ignoredColumns, List<ImportedTreeRow> rows) {
        this(recognizedFields, ignoredColumns, rows, List.of());
    }

    public ParsedTreeImportFile withFileWarning(TreeImportIssue warning) {
        List<TreeImportIssue> warnings = new java.util.ArrayList<>(fileWarnings);
        warnings.add(warning);
        return new ParsedTreeImportFile(recognizedFields, ignoredColumns, rows, List.copyOf(warnings));
    }
}
