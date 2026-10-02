package com.example.arbomachine.domain.treestand.facade;

import com.example.arbomachine.domain.treestand.dto.TreeStandRequest;
import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import com.example.arbomachine.domain.treestand.service.treestandfacade.AssignTreesInsideStandHandler;
import com.example.arbomachine.domain.treestand.service.treestandfacade.CreateTreeStandHandler;
import com.example.arbomachine.domain.treestand.service.treestandfacade.DeleteTreeStandHandler;
import com.example.arbomachine.domain.treestand.service.treestandfacade.GetTreeStandHandler;
import com.example.arbomachine.domain.treestand.service.treestandfacade.GetTreeStandsOfProjectHandler;
import com.example.arbomachine.domain.treestand.service.treestandfacade.UpdateTreeStandHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/** Grupy drzew (etap 29) w projektach zalogowanego uzytkownika - tylko deleguje do handlerow. */
@Component
public class TreeStandFacade {
    private final GetTreeStandsOfProjectHandler getTreeStandsOfProjectHandler;
    private final GetTreeStandHandler getTreeStandHandler;
    private final CreateTreeStandHandler createTreeStandHandler;
    private final UpdateTreeStandHandler updateTreeStandHandler;
    private final DeleteTreeStandHandler deleteTreeStandHandler;
    private final AssignTreesInsideStandHandler assignTreesInsideStandHandler;

    public TreeStandFacade(GetTreeStandsOfProjectHandler getTreeStandsOfProjectHandler, GetTreeStandHandler getTreeStandHandler,
                           CreateTreeStandHandler createTreeStandHandler, UpdateTreeStandHandler updateTreeStandHandler,
                           DeleteTreeStandHandler deleteTreeStandHandler, AssignTreesInsideStandHandler assignTreesInsideStandHandler) {
        this.getTreeStandsOfProjectHandler = getTreeStandsOfProjectHandler;
        this.getTreeStandHandler = getTreeStandHandler;
        this.createTreeStandHandler = createTreeStandHandler;
        this.updateTreeStandHandler = updateTreeStandHandler;
        this.deleteTreeStandHandler = deleteTreeStandHandler;
        this.assignTreesInsideStandHandler = assignTreesInsideStandHandler;
    }

    public List<TreeStandResponse> getStandsOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getTreeStandsOfProjectHandler.handle(currentUserEmail, projectId);
    }

    public TreeStandResponse getStandOfCurrentUser(String currentUserEmail, Long projectId, Long standId) {
        return getTreeStandHandler.handle(currentUserEmail, projectId, standId);
    }

    public TreeStandResponse createStandOfCurrentUser(String currentUserEmail, Long projectId, TreeStandRequest request) {
        return createTreeStandHandler.handle(currentUserEmail, projectId, request);
    }

    public TreeStandResponse updateStandOfCurrentUser(String currentUserEmail, Long projectId, Long standId, TreeStandRequest request) {
        return updateTreeStandHandler.handle(currentUserEmail, projectId, standId, request);
    }

    public void deleteStandOfCurrentUser(String currentUserEmail, Long projectId, Long standId) {
        deleteTreeStandHandler.handle(currentUserEmail, projectId, standId);
    }

    public TreeStandResponse assignTreesInsideStandOfCurrentUser(String currentUserEmail, Long projectId, Long standId) {
        return assignTreesInsideStandHandler.handle(currentUserEmail, projectId, standId);
    }
}
