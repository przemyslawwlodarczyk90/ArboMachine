package com.example.arbomachine.domain.treemarking.service.treemarkingfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsResponse;
import com.example.arbomachine.domain.treemarking.entity.TreeMarking;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Oznaczenia jednego drzewa z wlasnego projektu (cudze drzewo / projekt -> 404). */
@Component
public class GetTreeMarkingsHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeMarkingRepository treeMarkingRepository;
    private final TreeMarkingCatalog treeMarkingCatalog;

    public GetTreeMarkingsHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                  TreeMarkingRepository treeMarkingRepository, TreeMarkingCatalog treeMarkingCatalog) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeMarkingRepository = treeMarkingRepository;
        this.treeMarkingCatalog = treeMarkingCatalog;
    }

    @Transactional(readOnly = true)
    public TreeMarkingsResponse handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree ownedTree = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return new TreeMarkingsResponse(ownedTree.getId(), treeMarkingCatalog.inCatalogOrder(
                treeMarkingRepository.findAllByTreeId(ownedTree.getId()).stream().map(TreeMarking::getCode).toList()));
    }
}
