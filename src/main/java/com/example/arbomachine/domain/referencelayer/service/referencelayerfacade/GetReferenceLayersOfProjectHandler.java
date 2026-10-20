package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.referencelayer.dto.ReferenceLayerResponse;
import com.example.arbomachine.infrastructure.repository.ProjectReferenceLayerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Warstwy referencyjne wlasnego projektu w kolejnosci dodania (bez geometrii). */
@Component
public class GetReferenceLayersOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final ProjectReferenceLayerRepository projectReferenceLayerRepository;
    private final ReferenceLayerMapper referenceLayerMapper;

    public GetReferenceLayersOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                              ProjectReferenceLayerRepository projectReferenceLayerRepository,
                                              ReferenceLayerMapper referenceLayerMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.projectReferenceLayerRepository = projectReferenceLayerRepository;
        this.referenceLayerMapper = referenceLayerMapper;
    }

    @Transactional(readOnly = true)
    public List<ReferenceLayerResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return projectReferenceLayerRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .map(referenceLayerMapper::toResponse).toList();
    }
}
