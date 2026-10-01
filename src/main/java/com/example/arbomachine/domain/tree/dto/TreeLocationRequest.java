package com.example.arbomachine.domain.tree.dto;

import com.example.arbomachine.domain.tree.entity.TreeLocation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

/**
 * Body PUT /api/projects/{projectId}/trees/{treeId}/location - ustawienie albo zamiana punktu drzewa.
 * Obie wspolrzedne sa wymagane (brak stanu czesciowego); usuniecie lokalizacji to osobna operacja DELETE.
 * Od etapu 25 opcjonalnie dane pomiaru: wysokosc i dokladnosc w metrach oraz zrodlo punktu (MANUAL, MAP,
 * DEVICE_GNSS). Pominiete pola = brak danych; IMPORT nie jest dozwolony w tym endpoincie (ustawia go tylko import).
 */
public record TreeLocationRequest(
        @NotNull(message = "Latitude is mandatory")
        @DecimalMin(value = TreeLocation.MINIMUM_LATITUDE, message = "Latitude must be between -90 and 90")
        @DecimalMax(value = TreeLocation.MAXIMUM_LATITUDE, message = "Latitude must be between -90 and 90")
        BigDecimal latitude,

        @NotNull(message = "Longitude is mandatory")
        @DecimalMin(value = TreeLocation.MINIMUM_LONGITUDE, message = "Longitude must be between -180 and 180")
        @DecimalMax(value = TreeLocation.MAXIMUM_LONGITUDE, message = "Longitude must be between -180 and 180")
        BigDecimal longitude,

        @DecimalMin(value = TreeLocation.MINIMUM_ALTITUDE_M, message = "Altitude must be between -500 and 9000 m")
        @DecimalMax(value = TreeLocation.MAXIMUM_ALTITUDE_M, message = "Altitude must be between -500 and 9000 m")
        BigDecimal altitudeM,

        @DecimalMin(value = TreeLocation.MINIMUM_ACCURACY_M, message = "Accuracy must be between 0 and 10000 m")
        @DecimalMax(value = TreeLocation.MAXIMUM_ACCURACY_M, message = "Accuracy must be between 0 and 10000 m")
        BigDecimal accuracyM,

        @Pattern(regexp = "MANUAL|MAP|DEVICE_GNSS", message = "Unsupported location source")
        String source
) {

    /** Dotychczasowe wywolania (sam punkt, bez danych pomiaru). */
    public TreeLocationRequest(BigDecimal latitude, BigDecimal longitude) {
        this(latitude, longitude, null, null, null);
    }
}
