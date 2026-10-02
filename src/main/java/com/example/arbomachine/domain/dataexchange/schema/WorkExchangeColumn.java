package com.example.arbomachine.domain.dataexchange.schema;

import java.util.List;

/**
 * Kolumny eksportu prac (etap 28) - JEDNO miejsce dla arkusza XLSX "Prace" i pliku CSV prac (jeden wiersz = jedna
 * praca; drzewo moze miec wiele prac). Nazwy techniczne stabilne (jak TreeExchangeField), naglowki po polsku.
 * Status i priorytet eksportowane jako kody (RECOMMENDED..., LOW...), daty jako RRRR-MM-DD.
 */
public enum WorkExchangeColumn {
    TREE_ID("treeId", "ID drzewa (ArboMachine)"),
    INVENTORY_NUMBER("inventoryNumber", "Numer inwentaryzacyjny"),
    SPECIES_POLISH("speciesPolish", "Gatunek polski"),
    SPECIES_LATIN("speciesLatin", "Gatunek łaciński"),
    WORK_ID("workId", "ID pracy (ArboMachine)"),
    WORK_NAME("workName", "Praca"),
    WORK_DESCRIPTION("workDescription", "Opis / uzasadnienie"),
    WORK_STATUS("workStatus", "Status"),
    WORK_PRIORITY("workPriority", "Priorytet"),
    PLANNED_DATE("plannedDate", "Termin"),
    COMPLETED_DATE("completedDate", "Data wykonania"),
    WORK_SYMBOL_CODE("workMapSymbolCode", "Symbol na mapie");

    public static final List<WorkExchangeColumn> COLUMNS = List.of(values());

    private final String technicalName;
    private final String polishLabel;

    WorkExchangeColumn(String technicalName, String polishLabel) {
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
