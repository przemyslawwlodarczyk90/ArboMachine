package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

/** Surowa wartosc z pliku (tekst; liczby z XLSX/GeoJSON jako zapis dziesietny) + kolumna, z ktorej pochodzi. */
public record ImportedCell(TreeExchangeField field, String columnName, String rawValue) {

    public boolean isBlank() {
        return rawValue == null || rawValue.isBlank();
    }
}
