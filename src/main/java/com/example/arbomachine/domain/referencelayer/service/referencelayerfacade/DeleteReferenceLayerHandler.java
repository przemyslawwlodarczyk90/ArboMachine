package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import com.example.arbomachine.infrastructure.repository.ProjectReferenceLayerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Usuniecie warstwy referencyjnej wlasnego projektu (drzewa i inne dane bez zmian). */
@Component
public class DeleteReferenceLayerHandler {
    private final FindReferenceLayerOfProjectOwnedByCurrentUserHandler findReferenceLayerOfProjectOwnedByCurrentUserHandler;
    private final ProjectReferenceLayerRepository projectReferenceLayerRepository;

    public DeleteReferenceLayerHandler(FindReferenceLayerOfProjectOwnedByCurrentUserHandler findReferenceLayerOfProjectOwnedByCurrentUserHandler,
                                       ProjectReferenceLayerRepository projectReferenceLayerRepository) {
        this.findReferenceLayerOfProjectOwnedByCurrentUserHandler = findReferenceLayerOfProjectOwnedByCurrentUserHandler;
        this.projectReferenceLayerRepository = projectReferenceLayerRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, Long projectId, Long layerId) {
        projectReferenceLayerRepository.delete(findReferenceLayerOfProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, layerId));
    }
}
