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
 * Czesciowa aktualizacja (PATCH) wlasnego projektu: nazwa, opis i dane zlecenia (etap 29); pola pominiete w request
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
        if (updateProjectRequest.clientName() != null) {
            ownedProject.setClientName(ProjectOrderFields.trimmedOrNull(updateProjectRequest.clientName()));
        }
        if (updateProjectRequest.siteAddress() != null) {
            ownedProject.setSiteAddress(ProjectOrderFields.trimmedOrNull(updateProjectRequest.siteAddress()));
        }
        if (updateProjectRequest.orderDate() != null) {
            ownedProject.setOrderDate(updateProjectRequest.orderDate().isBlank() ? null
                    : java.time.LocalDate.parse(updateProjectRequest.orderDate()));
        }
        if (updateProjectRequest.contractNumber() != null) {
            ownedProject.setContractNumber(ProjectOrderFields.trimmedOrNull(updateProjectRequest.contractNumber()));
        }
        if (updateProjectRequest.documentPurpose() != null) {
            ownedProject.setDocumentPurpose(ProjectOrderFields.trimmedOrNull(updateProjectRequest.documentPurpose()));
        }
        ownedProject.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return projectMapper.toProjectResponse(projectRepository.save(ownedProject));
    }
}
