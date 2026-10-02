package com.example.arbomachine.domain.treework.dto;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body POST /api/projects/{projectId}/trees/{treeId}/works. Celowo BEZ projectId/treeId (pochodza z URL)
 * i bez ownerId/userId (wlasnosc wynika z projektu zalogowanego uzytkownika). Nieznane pola w JSON sa ignorowane.
 */
public record CreateTreeWorkRequest(
        @NotBlank(message = "Work name is mandatory")
        @Size(max = TreeWork.MAXIMUM_NAME_LENGTH, message = "Work name must have at most 255 characters")
        String name,

        @Size(max = TreeWork.MAXIMUM_DESCRIPTION_LENGTH, message = "Work description must have at most 5000 characters")
        String description,

        /** RECOMMENDED / PLANNED / COMPLETED / CANCELLED; brak = RECOMMENDED (etap 28). */
        @Pattern(regexp = "RECOMMENDED|PLANNED|COMPLETED|CANCELLED", message = "Unsupported work status")
        String status,

        /** LOW / NORMAL / HIGH / URGENT albo brak - priorytet organizacyjny V1. */
        @Pattern(regexp = "LOW|NORMAL|HIGH|URGENT", message = "Unsupported work priority")
        String priority,

        /** Planowany termin (RRRR-MM-DD), opcjonalny, moze byc w przeszlosci. */
        LocalDate plannedDate,

        /** Data wykonania (RRRR-MM-DD), opcjonalna. */
        LocalDate completedDate
) implements TreeWorkFieldsRequest {

    /** Dotychczasowe wywolania (sama nazwa i opis) - status RECOMMENDED, bez priorytetu i terminow. */
    public CreateTreeWorkRequest(String name, String description) {
        this(name, description, null, null, null, null);
    }
}
