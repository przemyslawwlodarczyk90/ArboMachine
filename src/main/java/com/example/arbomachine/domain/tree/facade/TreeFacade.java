package com.example.arbomachine.domain.tree.facade;

import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolRequest;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.dto.TreeLocationRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.dto.UpdateTreeRequest;
import com.example.arbomachine.domain.tree.service.treefacade.ClearTreeLocationHandler;
import com.example.arbomachine.domain.tree.service.treefacade.ClearTreeMapSymbolHandler;
import com.example.arbomachine.domain.tree.service.treefacade.CreateTreeHandler;
import com.example.arbomachine.domain.tree.service.treefacade.GetTreeHandler;
import com.example.arbomachine.domain.tree.service.treefacade.GetTreesOfProjectHandler;
import com.example.arbomachine.domain.tree.service.treefacade.SetTreeLocationHandler;
import com.example.arbomachine.domain.tree.service.treefacade.SetTreeMapSymbolHandler;
import com.example.arbomachine.domain.tree.service.treefacade.UpdateTreeHandler;
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
    private final UpdateTreeHandler updateTreeHandler;
    private final SetTreeLocationHandler setTreeLocationHandler;
    private final ClearTreeLocationHandler clearTreeLocationHandler;
    private final SetTreeMapSymbolHandler setTreeMapSymbolHandler;
    private final ClearTreeMapSymbolHandler clearTreeMapSymbolHandler;

    public TreeFacade(CreateTreeHandler createTreeHandler,
                      GetTreesOfProjectHandler getTreesOfProjectHandler,
                      GetTreeHandler getTreeHandler,
                      UpdateTreeHandler updateTreeHandler,
                      SetTreeLocationHandler setTreeLocationHandler,
                      ClearTreeLocationHandler clearTreeLocationHandler,
                      SetTreeMapSymbolHandler setTreeMapSymbolHandler,
                      ClearTreeMapSymbolHandler clearTreeMapSymbolHandler) {
        this.createTreeHandler = createTreeHandler;
        this.getTreesOfProjectHandler = getTreesOfProjectHandler;
        this.getTreeHandler = getTreeHandler;
        this.updateTreeHandler = updateTreeHandler;
        this.setTreeLocationHandler = setTreeLocationHandler;
        this.clearTreeLocationHandler = clearTreeLocationHandler;
        this.setTreeMapSymbolHandler = setTreeMapSymbolHandler;
        this.clearTreeMapSymbolHandler = clearTreeMapSymbolHandler;
    }

    public TreeResponse createTreeInProjectOfCurrentUser(String currentUserEmail, Long projectId,
                                                         CreateTreeRequest createTreeRequest) {
        return createTreeHandler.handle(currentUserEmail, projectId, createTreeRequest, null);
    }

    /** Etap 36: tworzenie z identyfikatorem urzadzenia (ponowienie bez duplikatu). */
    public TreeResponse createTreeInProjectOfCurrentUser(String currentUserEmail, Long projectId,
                                                         CreateTreeRequest createTreeRequest, String clientRequestId) {
        return createTreeHandler.handle(currentUserEmail, projectId, createTreeRequest, clientRequestId);
    }

    public List<TreeResponse> getTreesOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getTreesOfProjectHandler.handle(currentUserEmail, projectId);
    }

    public TreeResponse getTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreeHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreeResponse updateTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                        UpdateTreeRequest updateTreeRequest) {
        return updateTreeHandler.handle(currentUserEmail, projectId, treeId, updateTreeRequest);
    }

    public TreeResponse setLocationOfTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                               TreeLocationRequest treeLocationRequest) {
        return setTreeLocationHandler.handle(currentUserEmail, projectId, treeId, treeLocationRequest);
    }

    public TreeResponse clearLocationOfTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return clearTreeLocationHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreeResponse setMapSymbolOfTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                MapSymbolRequest mapSymbolRequest) {
        return setTreeMapSymbolHandler.handle(currentUserEmail, projectId, treeId, mapSymbolRequest);
    }

    public TreeResponse clearMapSymbolOfTreeOfProjectOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return clearTreeMapSymbolHandler.handle(currentUserEmail, projectId, treeId);
    }
}
