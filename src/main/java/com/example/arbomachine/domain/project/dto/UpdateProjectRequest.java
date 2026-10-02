package com.example.arbomachine.domain.project.dto;

import com.example.arbomachine.domain.project.entity.Project;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body PATCH /api/projects/{projectId}. Pole pominiete (albo null) = bez zmian.
 * Wlasciciela, id i daty utworzenia nie da sie tu zmienic - takich pol po prostu nie ma.
 */
public record UpdateProjectRequest(
        /** Jesli podana, nie moze byc pusta ani skladac sie z samych bialych znakow. */
        @Pattern(regexp = "(?s).*\\S.*", message = "Project name must not be blank")
        @Size(max = Project.MAXIMUM_NAME_LENGTH, message = "Project name must have at most 255 characters")
        String name,

        String description,

        /** Zleceniodawca: null = bez zmian, pusty tekst = usuniecie (etap 29). */
        @Size(max = Project.MAXIMUM_CLIENT_NAME_LENGTH, message = "Client name must have at most 255 characters")
        String clientName,

        /** Adres obiektu: null = bez zmian, pusty tekst = usuniecie. */
        @Size(max = Project.MAXIMUM_SITE_ADDRESS_LENGTH, message = "Site address must have at most 500 characters")
        String siteAddress,

        /** Data zlecenia RRRR-MM-DD: null = bez zmian, pusty tekst = usuniecie (tekst, bo pustej daty JSON nie wyrazi). */
        @Pattern(regexp = "|\\d{4}-\\d{2}-\\d{2}", message = "Order date must have format YYYY-MM-DD")
        String orderDate
) {

    /** Dotychczasowe wywolania (nazwa i opis). */
    public UpdateProjectRequest(String name, String description) {
        this(name, description, null, null, null);
    }
}
