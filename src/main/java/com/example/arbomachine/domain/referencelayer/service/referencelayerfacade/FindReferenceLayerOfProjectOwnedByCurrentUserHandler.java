package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.referencelayer.entity.ProjectReferenceLayer;
import com.example.arbomachine.domain.referencelayer.exception.ReferenceLayerNotFoundException;
import com.example.arbomachine.infrastructure.repository.ProjectReferenceLayerRepository;
import org.springframework.stereotype.Component;

/** Warstwa o podanym id DOKLADNIE z projektu z URL, a projekt - zalogowanego uzytkownika; inaczej 404. */
@Component
public class FindReferenceLayerOfProjectOwnedByCurrentUserHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectReferenceLayerRepository projectReferenceLayerRepository;

    public FindReferenceLayerOfProjectOwnedByCurrentUserHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                                                ProjectReferenceLayerRepository projectReferenceLayerRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectReferenceLayerRepository = projectReferenceLayerRepository;
    }

    public ProjectReferenceLayer handle(String currentUserEmail, Long projectId, Long layerId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return projectReferenceLayerRepository.findByIdAndProjectId(layerId, ownedProject.getId())
                .orElseThrow(() -> new ReferenceLayerNotFoundException("Reference layer not found"));
    }
}
