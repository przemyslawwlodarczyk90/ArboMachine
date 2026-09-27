package com.example.arbomachine.domain.tree.dto;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body POST /api/projects/{projectId}/trees. Celowo BEZ projectId (pochodzi z URL) i bez ownerId/userId
 * (wlasnosc wynika z projektu zalogowanego uzytkownika). Nieznane pola w JSON sa ignorowane.
 * Wymagany jest tylko numer inwentaryzacyjny - gatunek mozna uzupelnic pozniej.
 */
public record CreateTreeRequest(
        @NotBlank(message = "Inventory number is mandatory")
        @Size(max = Tree.MAXIMUM_INVENTORY_NUMBER_LENGTH, message = "Inventory number must have at most 100 characters")
        String inventoryNumber,

        @Size(max = Tree.MAXIMUM_SPECIES_LATIN_LENGTH, message = "Latin species name must have at most 255 characters")
        String speciesLatin,

        @Size(max = Tree.MAXIMUM_SPECIES_POLISH_LENGTH, message = "Polish species name must have at most 255 characters")
        String speciesPolish,

        @Digits(integer = 4, fraction = 0, message = "Trunk circumference must be a whole number of centimeters")
        @DecimalMin(value = "" + Tree.MINIMUM_TRUNK_CIRCUMFERENCE_CM, message = "Trunk circumference must be at least 1 cm")
        @DecimalMax(value = "" + Tree.MAXIMUM_TRUNK_CIRCUMFERENCE_CM, message = "Trunk circumference must be at most 5000 cm")
        BigDecimal trunkCircumferenceCm,

        String notes
) implements TreeEditableFieldsRequest {}
