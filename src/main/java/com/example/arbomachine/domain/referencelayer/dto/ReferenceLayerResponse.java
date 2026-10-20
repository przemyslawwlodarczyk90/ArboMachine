package com.example.arbomachine.domain.referencelayer.dto;

import java.time.Instant;

/** Warstwa referencyjna bez geometrii (lista); geometria osobno: GET .../reference-layers/{id}/geojson. */
public record ReferenceLayerResponse(
        Long id,
        Long projectId,
        String name,
        String sourceFileName,
        String sourceFormat,
        String sourceCrs,
        int featureCount,
        Instant createdAt
) {}
