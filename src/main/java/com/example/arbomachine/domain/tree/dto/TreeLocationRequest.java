package com.example.arbomachine.domain.tree.dto;

import com.example.arbomachine.domain.tree.entity.TreeLocation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Body PUT /api/projects/{projectId}/trees/{treeId}/location - ustawienie albo zamiana punktu drzewa.
 * Obie wspolrzedne sa wymagane (brak stanu czesciowego); usuniecie lokalizacji to osobna operacja DELETE.
 * Endpoint nie wie, skad pochodza wspolrzedne (formularz, mapa, GNSS) - dostaje tylko punkt w WGS84.
 */
public record TreeLocationRequest(
        @NotNull(message = "Latitude is mandatory")
        @DecimalMin(value = TreeLocation.MINIMUM_LATITUDE, message = "Latitude must be between -90 and 90")
        @DecimalMax(value = TreeLocation.MAXIMUM_LATITUDE, message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @NotNull(message = "Longitude is mandatory")
        @DecimalMin(value = TreeLocation.MINIMUM_LONGITUDE, message = "Longitude must be between -180 and 180")
        @DecimalMax(value = TreeLocation.MAXIMUM_LONGITUDE, message = "Longitude must be between -180 and 180")
        BigDecimal longitude
) {}
