package com.example.arbomachine.domain.dataexchange.imports;

/**
 * Odczyt pliku jednego formatu do wspolnego modelu wierszy. Nowy format (np. GeoPackage) = nowa implementacja;
 * walidacja i zapis drzew zostaja bez zmian. Problem z calym plikiem -> TreeImportFileException.
 */
public interface TreeImportParser {

    TreeImportFormat format();

    ParsedTreeImportFile parse(byte[] content);
}
