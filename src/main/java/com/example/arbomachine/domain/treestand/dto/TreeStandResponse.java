package com.example.arbomachine.domain.treestand.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Grupa drzew w API: geometria GeoJSON (WGS 84), pole w m2 (przyblizone), id przypisanych drzew rosnaco. */
public record TreeStandResponse(
        Long id,
        Long projectId,
        String name,
        String notes,
        JsonNode geometry,
        BigDecimal areaSquareMeters,
        String source,
        List<Long> treeIds,
        Instant createdAt,
        Instant updatedAt
) {}
