package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.util.List;
import java.util.Set;

/**
 * Wynik odczytu pliku (dowolnego formatu): rozpoznane pola, kolumny pominiete (nieznane albo tylko eksportowe,
 * np. "ID drzewa (ArboMachine)") i niepuste wiersze danych.
 */
public record ParsedTreeImportFile(Set<TreeExchangeField> recognizedFields, List<String> ignoredColumns,
                                   List<ImportedTreeRow> rows) {}
