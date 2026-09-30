package com.example.arbomachine.domain.tree.dto;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Body PATCH /api/projects/{projectId}/trees/{treeId} - osobna operacja niz tworzenie drzewa.
 * <p>
 * Swiadomie prosta semantyka: formularz edycji zawsze wysyla KOMPLET edytowalnych pol, wiec request zastepuje
 * je wszystkie. Numer inwentaryzacyjny jest wymagany jak przy tworzeniu; pusty lub pominiety gatunek/notatka
 * oznacza ich wyczyszczenie. Bez projectId/ownerId/treeId - identyfikatory pochodza z URL i tokenu,
 * a przeniesienie drzewa do innego projektu nie jest tu mozliwe.
 */
public record UpdateTreeRequest(
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

        @Digits(integer = 3, fraction = Tree.MEASUREMENT_IN_METERS_SCALE, message = "Height can have at most 2 decimal places")
        @DecimalMin(value = "0", inclusive = false, message = "Height must be greater than 0 m")
        @DecimalMax(value = Tree.MAXIMUM_HEIGHT_M, message = "Height must be at most 150 m")
        BigDecimal heightM,

        @Digits(integer = 3, fraction = Tree.MEASUREMENT_IN_METERS_SCALE, message = "Crown diameter can have at most 2 decimal places")
        @DecimalMin(value = "0", inclusive = false, message = "Crown diameter must be greater than 0 m")
        @DecimalMax(value = Tree.MAXIMUM_CROWN_DIAMETER_M, message = "Crown diameter must be at most 200 m")
        BigDecimal crownDiameterM,

        String notes,

        /** Opis stanu drzewa (V1: wolny tekst, bez skali i enumow - do potwierdzenia z arborysta). */
        String conditionNotes,

        /** Zagrozenia stwarzane przez drzewo (V1: wolny tekst, bez oceny ryzyka). */
        String hazardNotes
) implements TreeEditableFieldsRequest {}
