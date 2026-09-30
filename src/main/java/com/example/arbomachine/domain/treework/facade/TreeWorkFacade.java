package com.example.arbomachine.domain.treework.facade;

import com.example.arbomachine.domain.treework.dto.CreateTreeWorkRequest;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.dto.UpdateTreeWorkRequest;
import com.example.arbomachine.domain.treework.service.treeworkfacade.CreateTreeWorkHandler;
import com.example.arbomachine.domain.treework.service.treeworkfacade.DeleteTreeWorkHandler;
import com.example.arbomachine.domain.treework.service.treeworkfacade.GetTreeWorkHandler;
import com.example.arbomachine.domain.treework.service.treeworkfacade.GetTreeWorksHandler;
import com.example.arbomachine.domain.treework.service.treeworkfacade.UpdateTreeWorkHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Zalecenia / prace przy drzewach w projektach zalogowanego uzytkownika. Wlasnosc wynika z hierarchii
 * TreeWork -> Tree -> Project -> owner. Tylko deleguje do handlerow z service/treeworkfacade.
 */
@Component
public class TreeWorkFacade {
    private final CreateTreeWorkHandler createTreeWorkHandler;
    private final GetTreeWorksHandler getTreeWorksHandler;
    private final GetTreeWorkHandler getTreeWorkHandler;
    private final UpdateTreeWorkHandler updateTreeWorkHandler;
    private final DeleteTreeWorkHandler deleteTreeWorkHandler;

    public TreeWorkFacade(CreateTreeWorkHandler createTreeWorkHandler,
                          GetTreeWorksHandler getTreeWorksHandler,
                          GetTreeWorkHandler getTreeWorkHandler,
                          UpdateTreeWorkHandler updateTreeWorkHandler,
                          DeleteTreeWorkHandler deleteTreeWorkHandler) {
        this.createTreeWorkHandler = createTreeWorkHandler;
        this.getTreeWorksHandler = getTreeWorksHandler;
        this.getTreeWorkHandler = getTreeWorkHandler;
        this.updateTreeWorkHandler = updateTreeWorkHandler;
        this.deleteTreeWorkHandler = deleteTreeWorkHandler;
    }

    public TreeWorkResponse createWorkForTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                           CreateTreeWorkRequest createTreeWorkRequest) {
        return createTreeWorkHandler.handle(currentUserEmail, projectId, treeId, createTreeWorkRequest);
    }

    public List<TreeWorkResponse> getWorksOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreeWorksHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreeWorkResponse getWorkOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                       Long treeWorkId) {
        return getTreeWorkHandler.handle(currentUserEmail, projectId, treeId, treeWorkId);
    }

    public TreeWorkResponse updateWorkOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                          Long treeWorkId, UpdateTreeWorkRequest updateTreeWorkRequest) {
        return updateTreeWorkHandler.handle(currentUserEmail, projectId, treeId, treeWorkId, updateTreeWorkRequest);
    }

    public void deleteWorkOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId) {
        deleteTreeWorkHandler.handle(currentUserEmail, projectId, treeId, treeWorkId);
    }
}
