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

        String description
) {}
