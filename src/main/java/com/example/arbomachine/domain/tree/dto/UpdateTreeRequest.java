package com.example.arbomachine.domain.tree.dto;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

        String notes
) implements TreeEditableFieldsRequest {}
