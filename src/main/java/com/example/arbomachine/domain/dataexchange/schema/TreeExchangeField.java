package com.example.arbomachine.domain.dataexchange.schema;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JEDYNE miejsce mapowania pol drzewa w wymianie danych (etap 23-24) - "ArboMachine Tree Exchange Schema V1":
 * - technicalName - stabilna nazwa w GeoJSON i API (camelCase, jak w TreeResponse),
 * - gisFieldName  - krotka nazwa pola w eksportach GIS (GeoPackage; "QGIS compatibility profile V1"),
 * - polishLabel   - naglowek kolumny w CSV/XLSX (UI po polsku).
 * Nazw technicznych i GIS nie zmieniamy - pliki uzytkownikow i projekty QGIS na nich polegaja. Nowe pole = nowa stala.
 * importable = false: pole tylko eksportowane (identyfikator ArboMachine, dane pomiaru lokalizacji, symbol, prace) -
 * import je ignoruje (punkt z importu dostaje zrodlo IMPORT).
 */
public enum TreeExchangeField {
    TREE_ID("treeId", "tree_id", "ID drzewa (ArboMachine)", ValueType.INTEGER, false),
    INVENTORY_NUMBER("inventoryNumber", "inventory_no", "Numer inwentaryzacyjny", ValueType.TEXT, true),
    SPECIES_POLISH("speciesPolish", "species_pl", "Gatunek polski", ValueType.TEXT, true),
    SPECIES_LATIN("speciesLatin", "species_la", "Gatunek łaciński", ValueType.TEXT, true),
    TRUNK_CIRCUMFERENCE_CM("trunkCircumferenceCm", "circum_cm", "Obwód pnia [cm]", ValueType.INTEGER, true),
    HEIGHT_M("heightM", "height_m", "Wysokość [m]", ValueType.DECIMAL, true),
    CROWN_DIAMETER_M("crownDiameterM", "crown_m", "Średnica korony [m]", ValueType.DECIMAL, true),
    NOTES("notes", "notes", "Notatki", ValueType.TEXT, true),
    CONDITION_NOTES("conditionNotes", "condition", "Stan drzewa", ValueType.TEXT, true),
    HAZARD_NOTES("hazardNotes", "hazards", "Zagrożenia", ValueType.TEXT, true),
    LATITUDE("latitude", "latitude", "Szerokość geograficzna", ValueType.DECIMAL, true),
    LONGITUDE("longitude", "longitude", "Długość geograficzna", ValueType.DECIMAL, true),
    LOCATION_ACCURACY_M("locationAccuracyM", "accuracy_m", "Dokładność lokalizacji [m]", ValueType.DECIMAL, false),
    LOCATION_ALTITUDE_M("locationAltitudeM", "altitude_m", "Wysokość n.p.m. [m]", ValueType.DECIMAL, false),
    LOCATION_SOURCE("locationSource", "loc_source", "Źródło lokalizacji", ValueType.TEXT, false),
    MAP_SYMBOL_CODE("mapSymbolCode", "map_symbol", "Symbol drzewa na mapie", ValueType.TEXT, false),
    WORK_NAMES("workNames", "work_names", "Zalecenia i prace", ValueType.TEXT, false),
    // Waloryzacja (etap 27) - ocena arborysty; kategoria V1 to NIE skala branzowa (LOW / MEDIUM / HIGH / VERY_HIGH).
    VALUATION_CATEGORY("valuationCategory", "val_categ", "Waloryzacja - ocena ogólna", ValueType.TEXT, false),
    VALUATION_AGE_VALUE("valuationAgeValue", "val_age", "Wartość wiekowa", ValueType.TEXT, false),
    VALUATION_NATURAL_VALUE("valuationNaturalValue", "val_nature", "Wartość przyrodnicza", ValueType.TEXT, false),
    VALUATION_ECOLOGICAL_VALUE("valuationEcologicalValue", "val_ecol", "Wartość ekologiczna", ValueType.TEXT, false),
    VALUATION_CULTURAL_VALUE("valuationCulturalValue", "val_cult", "Wartość kulturowa", ValueType.TEXT, false),
    VALUATION_LANDSCAPE_VALUE("valuationLandscapeValue", "val_landsc", "Wartość krajobrazowa", ValueType.TEXT, false),
    VALUATION_JUSTIFICATION("valuationJustification", "val_justif", "Uzasadnienie waloryzacji", ValueType.TEXT, false),
    // Gospodarka drzewostanem (etap 28) - podsumowanie prac drzewa; szczegoly prac: arkusz "Prace" / CSV prac / works w GeoJSON.
    OPEN_WORK_COUNT("openWorkCount", "works_open", "Prace do wykonania (liczba)", ValueType.INTEGER, false),
    NEXT_PLANNED_WORK_DATE("nextPlannedWorkDate", "work_next", "Najbliższy termin pracy", ValueType.TEXT, false),
    // Etap 30: nazwa pracy do wykonania z najblizszym terminem (pierwsza dodana przy rownych terminach).
    NEXT_PLANNED_WORK_NAME("nextPlannedWorkName", "work_nextn", "Najbliższa praca", ValueType.TEXT, false),
    // Monitoring stanu (etap 29) - ostatnia kontrola; ocena jako kod GOOD / FAIR / POOR (NIE skala branzowa).
    // Pelna historia kontroli: arkusz XLSX "Monitoring". Uwagi tylko w CSV/XLSX (dlugi tekst - nie do GIS).
    LAST_MONITORING_DATE("lastMonitoringDate", "mon_date", "Data ostatniej kontroli", ValueType.TEXT, false),
    LAST_HEALTH_STATUS("lastHealthStatus", "mon_status", "Ocena stanu (ostatnia kontrola)", ValueType.TEXT, false),
    LAST_MONITORING_NOTES("lastMonitoringNotes", "mon_notes", "Uwagi z ostatniej kontroli", ValueType.TEXT, false);

    /** Wersja schematu wymiany - zapisywana w eksportach (GeoJSON, XLSX, GeoPackage). */
    public static final String SCHEMA_VERSION = "1";

    /** Kolumny eksportu tabelarycznego (CSV, XLSX) w stalej kolejnosci. */
    public static final List<TreeExchangeField> TABULAR_EXPORT_COLUMNS = List.of(TREE_ID, INVENTORY_NUMBER,
            SPECIES_POLISH, SPECIES_LATIN, TRUNK_CIRCUMFERENCE_CM, HEIGHT_M, CROWN_DIAMETER_M, NOTES, CONDITION_NOTES,
            HAZARD_NOTES, LATITUDE, LONGITUDE, LOCATION_ACCURACY_M, LOCATION_ALTITUDE_M, LOCATION_SOURCE, WORK_NAMES,
            VALUATION_CATEGORY, VALUATION_AGE_VALUE, VALUATION_NATURAL_VALUE, VALUATION_ECOLOGICAL_VALUE,
            VALUATION_CULTURAL_VALUE, VALUATION_LANDSCAPE_VALUE, VALUATION_JUSTIFICATION, OPEN_WORK_COUNT, NEXT_PLANNED_WORK_DATE,
            NEXT_PLANNED_WORK_NAME, LAST_MONITORING_DATE, LAST_HEALTH_STATUS, LAST_MONITORING_NOTES);

    /**
     * Atrybuty punktu w eksportach GIS (wspolrzedne sa w geometrii, nie w atrybutach). Z waloryzacji tylko kategoria -
     * dlugie opisy zostaja w CSV/XLSX.
     */
    public static final List<TreeExchangeField> GIS_ATTRIBUTES = List.of(TREE_ID, INVENTORY_NUMBER, SPECIES_POLISH,
            SPECIES_LATIN, TRUNK_CIRCUMFERENCE_CM, HEIGHT_M, CROWN_DIAMETER_M, NOTES, CONDITION_NOTES, HAZARD_NOTES,
            LOCATION_ACCURACY_M, LOCATION_ALTITUDE_M, LOCATION_SOURCE, MAP_SYMBOL_CODE, WORK_NAMES, VALUATION_CATEGORY,
            OPEN_WORK_COUNT, NEXT_PLANNED_WORK_DATE, NEXT_PLANNED_WORK_NAME, LAST_MONITORING_DATE, LAST_HEALTH_STATUS);

    public enum ValueType { TEXT, INTEGER, DECIMAL }

    private final String technicalName;
    private final String gisFieldName;
    private final String polishLabel;
    private final ValueType valueType;
    private final boolean importable;

    TreeExchangeField(String technicalName, String gisFieldName, String polishLabel, ValueType valueType, boolean importable) {
        this.technicalName = technicalName;
        this.gisFieldName = gisFieldName;
        this.polishLabel = polishLabel;
        this.valueType = valueType;
        this.importable = importable;
    }

    public String technicalName() {
        return technicalName;
    }

    public String gisFieldName() {
        return gisFieldName;
    }

    public String polishLabel() {
        return polishLabel;
    }

    public ValueType valueType() {
        return valueType;
    }

    public boolean importable() {
        return importable;
    }

    /**
     * Pole po naglowku kolumny / nazwie wlasciwosci z pliku: polska etykieta, nazwa techniczna albo nazwa GIS,
     * bez rozrozniania wielkosci liter i bialych znakow na brzegach - plik wyeksportowany z ArboMachine (albo
     * z QGIS) wraca bez zmian naglowkow.
     */
    public static Optional<TreeExchangeField> fromHeader(String header) {
        if (header == null) return Optional.empty();
        String normalizedHeader = normalized(header);
        return Arrays.stream(values())
                .filter(field -> normalized(field.polishLabel).equals(normalizedHeader)
                        || normalized(field.technicalName).equals(normalizedHeader)
                        || normalized(field.gisFieldName).equals(normalizedHeader))
                .findFirst();
    }

    private static String normalized(String text) {
        return text.replace("﻿", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
