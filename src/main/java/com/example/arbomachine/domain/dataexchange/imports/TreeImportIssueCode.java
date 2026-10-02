package com.example.arbomachine.domain.dataexchange.imports;

/**
 * Kody problemow importu drzew. API zwraca kod + wiersz + kolumne, a polskie zdanie sklada frontend
 * (techniczne komunikaty po angielsku, UI po polsku - jak w calym projekcie). Nigdy nazwy wyjatkow Javy.
 */
public enum TreeImportIssueCode {
    // --- caly plik ---
    UNSUPPORTED_FILE_FORMAT,
    FILE_TOO_LARGE,
    EMPTY_FILE,
    UNREADABLE_FILE,
    INVALID_ENCODING,
    INVALID_GEOJSON,
    UNSUPPORTED_CRS,
    MISSING_REQUIRED_COLUMN,
    NO_DATA_ROWS,
    TOO_MANY_ROWS,
    /** etap 29: plik GIS bez obiektow punktowych (np. same obszary - do importu grup drzew) */
    NO_POINT_FEATURES,
    /** etap 29: archiwum ZIP bez kompletu .shp */
    NO_SHAPEFILE_IN_ZIP,
    /** etap 29: import grup drzew - plik bez wielokatow */
    NO_POLYGON_FEATURES,
    // --- wiersz (blad - wiersz nie moze byc zaimportowany) ---
    REQUIRED_VALUE_MISSING,
    TEXT_TOO_LONG,
    INVALID_NUMBER,
    NOT_A_WHOLE_NUMBER,
    TOO_MANY_DECIMAL_PLACES,
    VALUE_OUT_OF_RANGE,
    INCOMPLETE_LOCATION,
    UNSUPPORTED_GEOMETRY,
    /** etap 29: obszar grupy drzew z bledna geometria (value = powod) */
    INVALID_POLYGON,
    // --- ostrzezenia (import mozliwy) ---
    DUPLICATE_INVENTORY_NUMBER_IN_FILE,
    INVENTORY_NUMBER_EXISTS_IN_PROJECT,
    /** etap 29: wspolrzedne przeliczone z ukladu pliku (column = kod EPSG) do WGS 84 */
    COORDINATES_CONVERTED,
    /** etap 29: numery drzew odczytane z opisow rysunku DXF (dopasowanie odnosnikow - do sprawdzenia) */
    NUMBERS_READ_FROM_DRAWING_LABELS,
    /** etap 29: wiersz arkusza opisuje obszar (grupe krzewow/drzew), nie drzewo - pominiety */
    AREA_ROW_SKIPPED,
    /** etap 29: obwod wielopniowy ("57+34") - zapisany w notatkach, pole obwodu puste */
    MULTI_STEM_CIRCUMFERENCE_IN_NOTES
}
