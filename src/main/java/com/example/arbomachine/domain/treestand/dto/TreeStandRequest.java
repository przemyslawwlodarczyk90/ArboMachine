package com.example.arbomachine.domain.treestand.dto;

import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body POST / PUT .../stands - komplet danych grupy drzew. geometry: GeoJSON "Polygon" w WGS 84. treeIds - drzewa
 * TEGO projektu (inne -> 400); brak = zadne. assignTreesInside = dopisz drzewa, ktorych punkt lezy w obszarze.
 */
public record TreeStandRequest(
        @NotBlank(message = "Stand name is required")
        @Size(max = TreeStand.MAXIMUM_NAME_LENGTH, message = "Stand name must have at most 255 characters")
        String name,

        @Size(max = TreeStand.MAXIMUM_NOTES_LENGTH, message = "Stand notes must have at most 5000 characters")
        String notes,

        @NotNull(message = "Stand geometry is required")
        JsonNode geometry,

        List<Long> treeIds,

        Boolean assignTreesInside
) {}
