package com.example.arbomachine.domain.dataexchange.imports;

/**
 * Problem importu. rowNumber - numer wiersza arkusza / CSV (naglowek = 1) albo numer obiektu GeoJSON (od 1);
 * null = caly plik. field - nazwa techniczna pola (TreeExchangeField), column - naglowek tak, jak jest w pliku
 * uzytkownika (albo polska etykieta brakujacej kolumny), value - fragment blednej wartosci.
 */
public record TreeImportIssue(Integer rowNumber, String field, String column, String value, TreeImportIssueCode code) {

    private static final int MAXIMUM_VALUE_PREVIEW_LENGTH = 60;

    public static TreeImportIssue ofFile(TreeImportIssueCode code, String column) {
        return new TreeImportIssue(null, null, column, null, code);
    }

    public static TreeImportIssue ofCell(int rowNumber, ImportedCell cell, TreeImportIssueCode code) {
        String value = cell.rawValue();
        if (value != null && value.length() > MAXIMUM_VALUE_PREVIEW_LENGTH) {
            value = value.substring(0, MAXIMUM_VALUE_PREVIEW_LENGTH) + "…";
        }
        return new TreeImportIssue(rowNumber, cell.field().technicalName(), cell.columnName(), value, code);
    }
}
