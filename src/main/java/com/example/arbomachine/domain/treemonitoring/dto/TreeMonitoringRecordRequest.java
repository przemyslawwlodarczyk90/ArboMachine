package com.example.arbomachine.domain.treemonitoring.dto;

import com.example.arbomachine.domain.treemonitoring.entity.TreeMonitoringRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body POST .../monitoring i PUT .../monitoring/{recordId} - komplet pol wpisu (pominiete pole = brak wartosci).
 * Bez id drzewa, projektu i wlasciciela - pochodza z URL i JWT. healthStatus: GOOD / FAIR / POOR albo brak.
 */
public record TreeMonitoringRecordRequest(
        @NotNull(message = "Inspection date is required")
        LocalDate inspectionDate,

        @Pattern(regexp = "GOOD|FAIR|POOR", message = "Unsupported health status")
        String healthStatus,

        @Size(max = TreeMonitoringRecord.MAXIMUM_NOTES_LENGTH, message = "Monitoring notes must have at most 5000 characters")
        String notes
) {}
