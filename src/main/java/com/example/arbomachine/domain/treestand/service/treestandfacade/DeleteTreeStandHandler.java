package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Usuniecie grupy (drzewa zostaja - znika tylko przypisanie). Grupa innego projektu -> 404. */
@Component
public class DeleteTreeStandHandler {
    private final FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler;
    private final TreeStandRepository treeStandRepository;

    public DeleteTreeStandHandler(FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler,
                                  TreeStandRepository treeStandRepository) {
        this.findTreeStandHandler = findTreeStandHandler;
        this.treeStandRepository = treeStandRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, Long projectId, Long standId) {
        treeStandRepository.delete(findTreeStandHandler.handle(currentUserEmail, projectId, standId));
    }
}
