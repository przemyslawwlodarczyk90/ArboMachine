package com.example.arbomachine.domain.treemonitoring.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Wpis monitoringu zwracany przez API - samo id drzewa, bez zagniezdzonego drzewa. healthStatus null = nie okreslono. */
public record TreeMonitoringRecordResponse(
        Long id,
        Long treeId,
        LocalDate inspectionDate,
        String healthStatus,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {}
