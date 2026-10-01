package com.example.arbomachine.domain.treevaluation.facade;

import com.example.arbomachine.domain.treevaluation.dto.TreeValuationRequest;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationResponse;
import com.example.arbomachine.domain.treevaluation.service.treevaluationfacade.DeleteTreeValuationHandler;
import com.example.arbomachine.domain.treevaluation.service.treevaluationfacade.GetTreeValuationHandler;
import com.example.arbomachine.domain.treevaluation.service.treevaluationfacade.GetTreeValuationsOfProjectHandler;
import com.example.arbomachine.domain.treevaluation.service.treevaluationfacade.SaveTreeValuationHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Waloryzacja drzew (etap 27) - zapis oceny arborysty, bez algorytmu i bez wyceny pienieznej. Wlasnosc:
 * waloryzacja -> drzewo -> projekt -> owner. Tylko deleguje do handlerow.
 */
@Component
public class TreeValuationFacade {
    private final GetTreeValuationHandler getTreeValuationHandler;
    private final SaveTreeValuationHandler saveTreeValuationHandler;
    private final DeleteTreeValuationHandler deleteTreeValuationHandler;
    private final GetTreeValuationsOfProjectHandler getTreeValuationsOfProjectHandler;

    public TreeValuationFacade(GetTreeValuationHandler getTreeValuationHandler, SaveTreeValuationHandler saveTreeValuationHandler,
                               DeleteTreeValuationHandler deleteTreeValuationHandler,
                               GetTreeValuationsOfProjectHandler getTreeValuationsOfProjectHandler) {
        this.getTreeValuationHandler = getTreeValuationHandler;
        this.saveTreeValuationHandler = saveTreeValuationHandler;
        this.deleteTreeValuationHandler = deleteTreeValuationHandler;
        this.getTreeValuationsOfProjectHandler = getTreeValuationsOfProjectHandler;
    }

    public TreeValuationResponse getValuationOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreeValuationHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreeValuationResponse saveValuationOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                  TreeValuationRequest request) {
        return saveTreeValuationHandler.handle(currentUserEmail, projectId, treeId, request);
    }

    public void deleteValuationOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        deleteTreeValuationHandler.handle(currentUserEmail, projectId, treeId);
    }

    public List<TreeValuationResponse> getValuationsOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getTreeValuationsOfProjectHandler.handle(currentUserEmail, projectId);
    }
}
