package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

/** Usuniecie pracy - tylko z drzewa z URL w WLASNYM projekcie; inaczej 404 i nic nie jest usuwane. */
@Component
public class DeleteTreeWorkHandler {
    private final FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;

    public DeleteTreeWorkHandler(
            FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler,
            TreeWorkRepository treeWorkRepository) {
        this.findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId) {
        TreeWork treeWork = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId,
                treeId, treeWorkId);
        treeWorkRepository.delete(treeWork);
    }
}
