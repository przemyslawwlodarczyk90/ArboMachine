package com.example.arbomachine.domain.treemarking.facade;

import com.example.arbomachine.domain.treemarking.dto.TreeMarkingDefinitionResponse;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsRequest;
import com.example.arbomachine.domain.treemarking.dto.TreeMarkingsResponse;
import com.example.arbomachine.domain.treemarking.service.treemarkingfacade.GetTreeMarkingCatalogHandler;
import com.example.arbomachine.domain.treemarking.service.treemarkingfacade.GetTreeMarkingsHandler;
import com.example.arbomachine.domain.treemarking.service.treemarkingfacade.GetTreeMarkingsOfProjectHandler;
import com.example.arbomachine.domain.treemarking.service.treemarkingfacade.SetTreeMarkingsHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/** Oznaczenia drzew (etap 31) - katalog i oznaczenia drzew w projektach zalogowanego uzytkownika; tylko deleguje. */
@Component
public class TreeMarkingFacade {
    private final GetTreeMarkingCatalogHandler getTreeMarkingCatalogHandler;
    private final GetTreeMarkingsOfProjectHandler getTreeMarkingsOfProjectHandler;
    private final GetTreeMarkingsHandler getTreeMarkingsHandler;
    private final SetTreeMarkingsHandler setTreeMarkingsHandler;

    public TreeMarkingFacade(GetTreeMarkingCatalogHandler getTreeMarkingCatalogHandler,
                             GetTreeMarkingsOfProjectHandler getTreeMarkingsOfProjectHandler,
                             GetTreeMarkingsHandler getTreeMarkingsHandler, SetTreeMarkingsHandler setTreeMarkingsHandler) {
        this.getTreeMarkingCatalogHandler = getTreeMarkingCatalogHandler;
        this.getTreeMarkingsOfProjectHandler = getTreeMarkingsOfProjectHandler;
        this.getTreeMarkingsHandler = getTreeMarkingsHandler;
        this.setTreeMarkingsHandler = setTreeMarkingsHandler;
    }

    public List<TreeMarkingDefinitionResponse> getCatalog() {
        return getTreeMarkingCatalogHandler.handle();
    }

    public List<TreeMarkingsResponse> getMarkingsOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getTreeMarkingsOfProjectHandler.handle(currentUserEmail, projectId);
    }

    public TreeMarkingsResponse getMarkingsOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreeMarkingsHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreeMarkingsResponse setMarkingsOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                               TreeMarkingsRequest request) {
        return setTreeMarkingsHandler.handle(currentUserEmail, projectId, treeId, request);
    }
}
