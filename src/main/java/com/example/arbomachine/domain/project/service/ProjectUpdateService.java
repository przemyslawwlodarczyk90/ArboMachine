package com.example.arbomachine.domain.project.service;

import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Czesciowa aktualizacja (PATCH) projektu, ktory wywolujacy juz pobral z kontrola wlasnosci.
 * Zmienia tylko nazwe i opis; pola pominiete w request zostaja bez zmian.
 */
@Service
public class ProjectUpdateService {
    private final ProjectRepository projectRepository;

    public ProjectUpdateService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public Project applyChanges(Project ownedProject, UpdateProjectRequest updateProjectRequest) {
        if (updateProjectRequest.name() != null) {
            ownedProject.setName(updateProjectRequest.name());
        }
        if (updateProjectRequest.description() != null) {
            ownedProject.setDescription(updateProjectRequest.description());
        }
        ownedProject.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return projectRepository.save(ownedProject);
    }
}
