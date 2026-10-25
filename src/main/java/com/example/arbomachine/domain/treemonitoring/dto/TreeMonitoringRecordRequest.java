package com.example.arbomachine.domain.treemonitoring.dto;

import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body POST .../monitoring i PUT .../monitoring/{recordId} - komplet pol wpisu (pominiete pole = brak wartosci).
 * Bez id drzewa, projektu i wlasciciela - pochodza z URL i JWT. healthStatus: GOOD / FAIR / POOR albo brak.
 * Etap 51: nextInspectionDate - opcjonalny termin nastepnej kontroli, nie wczesniejszy niz data kontroli.
 */
public record TreeMonitoringRecordRequest(
        @NotNull(message = "Inspection date is required")
        LocalDate inspectionDate,

        @Pattern(regexp = "GOOD|FAIR|POOR", message = "Unsupported health status")
        String healthStatus,

        @Size(max = TreeMonitoringRecord.MAXIMUM_NOTES_LENGTH, message = "Monitoring notes must have at most 5000 characters")
        String notes,

        LocalDate nextInspectionDate
) {
    /** Etap 51: termin nastepnej kontroli nie moze byc przed data tej kontroli (formularz sprawdza to wczesniej). */
    @JsonIgnore
    @AssertTrue(message = "Next inspection date must not be before inspection date")
    public boolean isNextInspectionDateNotBeforeInspectionDate() {
        return nextInspectionDate == null || inspectionDate == null || !nextInspectionDate.isBefore(inspectionDate);
    }
}
