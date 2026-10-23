package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Usuniecie drzewa z WLASNEGO projektu (etap 44, 02 poz. 2.10 - omylkowo dodane drzewo): najpierw dane modulow
 * zaleznych (TreeDataCleaner), potem drzewo - wszystko w jednej transakcji. Cudzy projekt / drzewo spoza projektu /
 * drzewo juz usuniete -> 404. Bez kosza i bez cofania; drzewo wyciete to osobna sprawa (oznaczenie), nie usuniecie.
 */
@Component
public class DeleteTreeHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final List<TreeDataCleaner> treeDataCleaners;
    private final TreeRepository treeRepository;

    public DeleteTreeHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                             List<TreeDataCleaner> treeDataCleaners, TreeRepository treeRepository) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeDataCleaners = treeDataCleaners;
        this.treeRepository = treeRepository;
    }

    @Transactional
    public void handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree tree = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        treeDataCleaners.forEach(cleaner -> cleaner.deleteDataOf(tree));
        treeRepository.delete(tree);
    }
}
