package com.example.arbomachine.domain.projectdocument.data;

import java.time.LocalDate;

/**
 * Kontrola stanu drzewa w dokumencie (etap 29): data, kod oceny (GOOD/FAIR/POOR albo null = nie okreslono), uwagi.
 * Etap 51: termin nastepnej kontroli (null = brak).
 */
public record ProjectDocumentMonitoringData(LocalDate inspectionDate, String healthStatus, String notes,
                                            LocalDate nextInspectionDate) {}
