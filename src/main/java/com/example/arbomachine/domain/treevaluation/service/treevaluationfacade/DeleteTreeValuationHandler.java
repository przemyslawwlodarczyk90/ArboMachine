package com.example.arbomachine.domain.treevaluation.service.treevaluationfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Usuniecie waloryzacji drzewa z WLASNEGO projektu; brak waloryzacji to nie blad (idempotentnie - takze dla offline). */
@Component
public class DeleteTreeValuationHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeValuationRepository treeValuationRepository;

    public DeleteTreeValuationHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                      TreeValuationRepository treeValuationRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeValuationRepository = treeValuationRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree tree = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        treeValuationRepository.findByTreeId(tree.getId()).ifPresent(treeValuationRepository::delete);
    }
}
