package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.tree.entity.TreeEditableAttributes;
import com.example.arbomachine.domain.tree.entity.TreeLocation;

/** Poprawny wiersz importu - gotowe, znormalizowane pola nowego drzewa i opcjonalna lokalizacja. */
public record ValidatedTreeRow(int rowNumber, TreeEditableAttributes attributes, TreeLocation locationOrNull) {}
