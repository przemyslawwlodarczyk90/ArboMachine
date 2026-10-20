package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Drzewo w eksporcie - proste dane skopiowane z encji (eksportery nie znaja JPA). latitude/longitude null = brak
 * lokalizacji (WGS84, jak TreeLocation). valueOf(...) to jedyne miejsce laczace pole schematu z wartoscia.
 */
public record TreeExportData(
        long treeId,
        String inventoryNumber,
        String speciesPolish,
        String speciesLatin,
        Integer trunkCircumferenceCm,
        BigDecimal heightM,
        BigDecimal crownDiameterM,
        String notes,
        String conditionNotes,
        String hazardNotes,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal locationAccuracyM,
        BigDecimal locationAltitudeM,
        String locationSource,
        String mapSymbolCode,
        List<TreeWorkExportData> works,
        TreeValuationExportData valuation,
        /** etap 29: kontrole stanu, najnowsza pierwsza (pusta lista = drzewo nie sprawdzone) */
        List<TreeMonitoringExportData> monitoringRecords,
        /** etap 29: nazwy grup drzew, do ktorych nalezy drzewo */
        List<String> standNames,
        /** etap 31: nazwy oznaczen drzewa w kolejnosci katalogu (pusta lista = brak) */
        List<String> markingNames
) {

    public static final String STAND_NAMES_SEPARATOR = "; ";

    /** Separator nazw prac w jednej kolumnie / atrybucie. */
    public static final String WORK_NAMES_SEPARATOR = "; ";

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }

    /** Wartosc kolumny prac (WorkExchangeColumn) dla jednej pracy tego drzewa. */
    public Object workValueOf(TreeWorkExportData work, com.example.arbomachine.domain.dataexchange.schema.WorkExchangeColumn column) {
        return switch (column) {
            case TREE_ID -> treeId;
            case INVENTORY_NUMBER -> inventoryNumber;
            case SPECIES_POLISH -> speciesPolish;
            case SPECIES_LATIN -> speciesLatin;
            case WORK_ID -> work.workId();
            case WORK_NAME -> work.name();
            case WORK_DESCRIPTION -> work.description();
            case WORK_STATUS -> work.status();
            case WORK_PRIORITY -> work.priority();
            case PLANNED_DATE -> work.plannedDate() == null ? null : work.plannedDate().toString();
            case COMPLETED_DATE -> work.completedDate() == null ? null : work.completedDate().toString();
            case WORK_SYMBOL_CODE -> work.mapSymbolCode();
        };
    }

    /** Wartosc kolumny arkusza "Monitoring" (MonitoringExchangeColumn) dla jednej kontroli tego drzewa. */
    public Object monitoringValueOf(TreeMonitoringExportData record,
                                    com.example.arbomachine.domain.dataexchange.schema.MonitoringExchangeColumn column) {
        return switch (column) {
            case TREE_ID -> treeId;
            case INVENTORY_NUMBER -> inventoryNumber;
            case SPECIES_POLISH -> speciesPolish;
            case RECORD_ID -> record.recordId();
            case INSPECTION_DATE -> record.inspectionDate().toString();
            case HEALTH_STATUS -> record.healthStatus();
            case NOTES -> record.notes();
        };
    }

    /** Praca do wykonania z najblizszym terminem (pierwsza dodana przy rownych terminach) albo null. */
    public TreeWorkExportData nextPlannedWork() {
        return works.stream().filter(TreeWorkExportData::isOpen).filter(work -> work.plannedDate() != null)
                .min(java.util.Comparator.comparing(TreeWorkExportData::plannedDate)).orElse(null);
    }

    /** Ostatnia kontrola stanu albo null (drzewo nie sprawdzone). */
    public TreeMonitoringExportData latestMonitoringRecord() {
        return monitoringRecords.isEmpty() ? null : monitoringRecords.getFirst();
    }

    /** Wartosc pola: String, Long/Integer, BigDecimal albo null (brak danych). */
    public Object valueOf(TreeExchangeField field) {
        return switch (field) {
            case TREE_ID -> treeId;
            case INVENTORY_NUMBER -> inventoryNumber;
            case SPECIES_POLISH -> speciesPolish;
            case SPECIES_LATIN -> speciesLatin;
            case TRUNK_CIRCUMFERENCE_CM -> trunkCircumferenceCm;
            case HEIGHT_M -> heightM;
            case CROWN_DIAMETER_M -> crownDiameterM;
            case NOTES -> notes;
            case CONDITION_NOTES -> conditionNotes;
            case HAZARD_NOTES -> hazardNotes;
            case LATITUDE -> latitude;
            case LONGITUDE -> longitude;
            case LOCATION_ACCURACY_M -> locationAccuracyM;
            case LOCATION_ALTITUDE_M -> locationAltitudeM;
            case LOCATION_SOURCE -> locationSource;
            case MAP_SYMBOL_CODE -> mapSymbolCode;
            case WORK_NAMES -> works.isEmpty() ? null
                    : works.stream().map(TreeWorkExportData::name).collect(Collectors.joining(WORK_NAMES_SEPARATOR));
            case VALUATION_CATEGORY -> valuation == null ? null : valuation.overallCategory();
            case VALUATION_AGE_VALUE -> valuation == null ? null : valuation.ageValue();
            case VALUATION_NATURAL_VALUE -> valuation == null ? null : valuation.naturalValue();
            case VALUATION_ECOLOGICAL_VALUE -> valuation == null ? null : valuation.ecologicalValue();
            case VALUATION_CULTURAL_VALUE -> valuation == null ? null : valuation.culturalValue();
            case VALUATION_LANDSCAPE_VALUE -> valuation == null ? null : valuation.landscapeValue();
            case VALUATION_JUSTIFICATION -> valuation == null ? null : valuation.justification();
            case OPEN_WORK_COUNT -> (int) works.stream().filter(TreeWorkExportData::isOpen).count();
            case NEXT_PLANNED_WORK_DATE -> nextPlannedWork() == null ? null : nextPlannedWork().plannedDate().toString();
            case NEXT_PLANNED_WORK_NAME -> nextPlannedWork() == null ? null : nextPlannedWork().name();
            case LAST_MONITORING_DATE -> latestMonitoringRecord() == null ? null : latestMonitoringRecord().inspectionDate().toString();
            case LAST_HEALTH_STATUS -> latestMonitoringRecord() == null ? null : latestMonitoringRecord().healthStatus();
            case LAST_MONITORING_NOTES -> latestMonitoringRecord() == null ? null : latestMonitoringRecord().notes();
            case SPECIES_GROUP -> com.example.arbomachine.domain.dataexchange.schema.SpeciesGroupClassifier.groupOf(speciesLatin, speciesPolish);
            case STAND_NAMES -> standNames.isEmpty() ? null : String.join(STAND_NAMES_SEPARATOR, standNames);
            case MARKINGS -> markingNames.isEmpty() ? null : String.join(STAND_NAMES_SEPARATOR, markingNames);
        };
    }
}
