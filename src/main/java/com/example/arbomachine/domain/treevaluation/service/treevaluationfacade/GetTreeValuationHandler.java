package com.example.arbomachine.domain.treevaluation.service.treevaluationfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationResponse;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Waloryzacja drzewa z WLASNEGO projektu (cudzy projekt / drzewo spoza projektu -> 404); brak -> pusta (hasValuation=false). */
@Component
public class GetTreeValuationHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeValuationRepository treeValuationRepository;

    public GetTreeValuationHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                   TreeValuationRepository treeValuationRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeValuationRepository = treeValuationRepository;
    }

    @Transactional(readOnly = true)
    public TreeValuationResponse handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree tree = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treeValuationRepository.findByTreeId(tree.getId())
                .map(TreeValuationMapper::toResponse)
                .orElseGet(() -> TreeValuationResponse.emptyFor(tree.getId()));
    }
}
