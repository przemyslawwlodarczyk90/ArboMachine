package com.example.arbomachine.domain.tree.dto;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

        String notes
) implements TreeEditableFieldsRequest {}
