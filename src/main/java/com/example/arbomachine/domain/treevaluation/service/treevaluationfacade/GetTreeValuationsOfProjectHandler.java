package com.example.arbomachine.domain.treevaluation.service.treevaluationfacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationResponse;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Waloryzacje wszystkich drzew WLASNEGO projektu jednym zapytaniem (lista drzew z filtrem, mapa) - tylko drzewa,
 * ktore maja waloryzacje; brak na liscie = nie oceniono. Cudzy projekt -> 404.
 */
@Component
public class GetTreeValuationsOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeValuationRepository treeValuationRepository;

    public GetTreeValuationsOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                             TreeValuationRepository treeValuationRepository) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeValuationRepository = treeValuationRepository;
    }

    @Transactional(readOnly = true)
    public List<TreeValuationResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeValuationRepository.findAllByTreeProjectId(ownedProject.getId()).stream()
                .map(TreeValuationMapper::toResponse)
                .toList();
    }
}
