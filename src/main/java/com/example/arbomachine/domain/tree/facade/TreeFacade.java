package com.example.arbomachine.domain.tree.facade;

import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.service.treefacade.CreateTreeHandler;
import com.example.arbomachine.domain.tree.service.treefacade.GetTreeHandler;
import com.example.arbomachine.domain.tree.service.treefacade.GetTreesOfProjectHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Drzewa w projektach zalogowanego uzytkownika. Wlasnosc drzewa wynika z projektu (Tree -> Project -> owner).
 * Tylko deleguje do handlerow z service/treefacade.
 */
@Component
public class TreeFacade {
    private final CreateTreeHandler createTreeHandler;
    private final GetTreesOfProjectHandler getTreesOfProjectHandler;
    private final GetTreeHandler getTreeHandler;

    public TreeFacade(CreateTreeHandler createTreeHandler,
                      GetTreesOfProjectHandler getTreesOfProjectHandler,
                      GetTreeHandler getTreeHandler) {
        this.createTreeHandler = createTreeHandler;
        this.getTreesOfProjectHandler = getTreesOfProjectHandler;
        this.getTreeHandler = getTreeHandler;
    }

    public TreeResponse createTreeInProjectOfCurrentUser(String currentUserEmail, Long projectId,
                                                         CreateTreeRequest createTreeRequest) {
        return createTreeHandler.handle(currentUserEmail, projectId, createTreeRequest);
    }

    public List<TreeResponse> getTreesOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getTreesOfProjectHandler.handle(currentUserEmail, projectId);
    }

    public TreeResponse getTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreeHandler.handle(currentUserEmail, projectId, treeId);
    }
}
