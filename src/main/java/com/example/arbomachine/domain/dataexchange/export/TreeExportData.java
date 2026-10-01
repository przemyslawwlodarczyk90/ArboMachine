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
        String mapSymbolCode,
        List<TreeWorkExportData> works
) {

    /** Separator nazw prac w jednej kolumnie / atrybucie. */
    public static final String WORK_NAMES_SEPARATOR = "; ";

    public boolean hasLocation() {
        return latitude != null && longitude != null;
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
            case MAP_SYMBOL_CODE -> mapSymbolCode;
            case WORK_NAMES -> works.isEmpty() ? null
                    : works.stream().map(TreeWorkExportData::name).collect(Collectors.joining(WORK_NAMES_SEPARATOR));
        };
    }
}
