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

        String description
) {}
