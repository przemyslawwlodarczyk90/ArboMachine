package com.example.arbomachine.domain.project.dto;

import com.example.arbomachine.domain.project.entity.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body POST /api/projects. Celowo BEZ ownerId - wlascicielem jest zawsze zalogowany uzytkownik.
 * Dodatkowe pola w JSON (np. "ownerId") sa ignorowane przy deserializacji.
 */
public record CreateProjectRequest(
        @NotBlank(message = "Project name is mandatory")
        @Size(max = Project.MAXIMUM_NAME_LENGTH, message = "Project name must have at most 255 characters")
        String name,

        String description,

        /** Zleceniodawca (etap 29, opcjonalny). */
        @Size(max = Project.MAXIMUM_CLIENT_NAME_LENGTH, message = "Client name must have at most 255 characters")
        String clientName,

        /** Adres obiektu / lokalizacja prac (opcjonalny). */
        @Size(max = Project.MAXIMUM_SITE_ADDRESS_LENGTH, message = "Site address must have at most 500 characters")
        String siteAddress,

        /** Data zlecenia RRRR-MM-DD (opcjonalna). */
        java.time.LocalDate orderDate
) {

    /** Dotychczasowe wywolania (nazwa i opis). */
    public CreateProjectRequest(String name, String description) {
        this(name, description, null, null, null);
    }
}
