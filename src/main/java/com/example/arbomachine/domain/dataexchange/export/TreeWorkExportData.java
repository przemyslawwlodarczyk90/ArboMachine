package com.example.arbomachine.domain.dataexchange.export;

import java.time.LocalDate;

/**
 * Zalecenie / praca w eksporcie: nazwa, opis, kod symbolu mapy (atrybut - QGIS moze po nim stylowac) i - od etapu 28 -
 * status, priorytet (kody) oraz terminy.
 */
public record TreeWorkExportData(
        long workId,
        String name,
        String description,
        String mapSymbolCode,
        String status,
        String priority,
        LocalDate plannedDate,
        LocalDate completedDate
) {

    /** Praca do wykonania (zalecona albo zaplanowana) - jak TreeWorkStatus.isOpen(). */
    public boolean isOpen() {
        return "RECOMMENDED".equals(status) || "PLANNED".equals(status);
    }
}
