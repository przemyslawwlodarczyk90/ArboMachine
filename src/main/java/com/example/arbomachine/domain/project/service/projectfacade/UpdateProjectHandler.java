package com.example.arbomachine.domain.project.service.projectfacade;

import com.example.arbomachine.domain.project.dto.ProjectResponse;
import com.example.arbomachine.domain.project.dto.UpdateProjectRequest;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.mapper.ProjectMapper;
import com.example.arbomachine.infrastructure.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Czesciowa aktualizacja (PATCH) wlasnego projektu. Zmienia tylko nazwe i opis; pola pominiete w request
 * zostaja bez zmian. Wlasciciela, id i daty utworzenia nie da sie zmienic.
 */
@Component
public class UpdateProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public UpdateProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectResponse handle(String currentUserEmail, Long projectId, UpdateProjectRequest updateProjectRequest) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        if (updateProjectRequest.name() != null) {
            ownedProject.setName(updateProjectRequest.name());
        }
        if (updateProjectRequest.description() != null) {
            ownedProject.setDescription(updateProjectRequest.description());
        }
        ownedProject.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return projectMapper.toProjectResponse(projectRepository.save(ownedProject));
    }
}
