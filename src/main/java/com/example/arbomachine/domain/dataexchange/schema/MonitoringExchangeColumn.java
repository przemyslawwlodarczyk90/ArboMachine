package com.example.arbomachine.domain.dataexchange.schema;

import java.util.List;

/**
 * Kolumny arkusza XLSX "Monitoring" (etap 29) - jeden wiersz = jedna kontrola (drzewo ma historie kontroli).
 * Nazwy techniczne stabilne (jak TreeExchangeField), naglowki po polsku; ocena stanu jako kod (GOOD/FAIR/POOR).
 */
public enum MonitoringExchangeColumn {
    TREE_ID("treeId", "ID drzewa (ArboMachine)"),
    INVENTORY_NUMBER("inventoryNumber", "Numer inwentaryzacyjny"),
    SPECIES_POLISH("speciesPolish", "Gatunek polski"),
    RECORD_ID("monitoringRecordId", "ID kontroli (ArboMachine)"),
    INSPECTION_DATE("inspectionDate", "Data kontroli"),
    HEALTH_STATUS("healthStatus", "Ocena stanu"),
    NOTES("monitoringNotes", "Uwagi z kontroli");

    public static final List<MonitoringExchangeColumn> COLUMNS = List.of(values());

    private final String technicalName;
    private final String polishLabel;

    MonitoringExchangeColumn(String technicalName, String polishLabel) {
        this.technicalName = technicalName;
        this.polishLabel = polishLabel;
    }

    public String technicalName() {
        return technicalName;
    }

    public String polishLabel() {
        return polishLabel;
    }
}
