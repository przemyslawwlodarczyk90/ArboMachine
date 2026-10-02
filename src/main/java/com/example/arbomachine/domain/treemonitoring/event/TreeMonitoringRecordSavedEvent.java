package com.example.arbomachine.domain.treemonitoring.event;

import java.time.LocalDate;

/**
 * Zdarzenie po zapisie wpisu monitoringu (nowego albo zmienionego) - publikowane w transakcji zapisu, odbiorcy
 * (np. powiadomienia) reaguja po commit. Proste dane, bez encji: modul monitoringu nie zna odbiorcow.
 * previousHealthStatus - ocena przed edycja (null dla nowego wpisu albo gdy nie bylo oceny).
 */
public record TreeMonitoringRecordSavedEvent(
        long recordId,
        long ownerUserAccountId,
        long projectId,
        String projectName,
        long treeId,
        String treeInventoryNumber,
        LocalDate inspectionDate,
        String healthStatus,
        String previousHealthStatus,
        boolean newRecord
) {}
