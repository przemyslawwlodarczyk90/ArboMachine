package com.example.arbomachine.domain.dataexchange.export;

import java.time.LocalDate;

/**
 * Wpis monitoringu w eksporcie (etap 29): data kontroli, kod oceny stanu (GOOD/FAIR/POOR albo null) i uwagi.
 * Etap 51: termin nastepnej kontroli (null = brak).
 */
public record TreeMonitoringExportData(long recordId, LocalDate inspectionDate, String healthStatus, String notes,
                                       LocalDate nextInspectionDate) {}
