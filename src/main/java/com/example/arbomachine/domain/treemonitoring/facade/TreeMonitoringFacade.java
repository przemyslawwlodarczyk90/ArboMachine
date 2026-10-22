package com.example.arbomachine.domain.treemonitoring.facade;

import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordRequest;
import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade.CreateTreeMonitoringRecordHandler;
import com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade.DeleteTreeMonitoringRecordHandler;
import com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade.GetLatestTreeMonitoringRecordsOfProjectHandler;
import com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade.GetTreeMonitoringRecordHandler;
import com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade.GetTreeMonitoringRecordsHandler;
import com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade.UpdateTreeMonitoringRecordHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Monitoring stanu drzew (etap 29) w projektach zalogowanego uzytkownika. Wlasnosc wynika z hierarchii
 * wpis -> Tree -> Project -> owner. Tylko deleguje do handlerow z service/treemonitoringfacade.
 */
@Component
public class TreeMonitoringFacade {
    private final CreateTreeMonitoringRecordHandler createTreeMonitoringRecordHandler;
    private final GetTreeMonitoringRecordsHandler getTreeMonitoringRecordsHandler;
    private final GetTreeMonitoringRecordHandler getTreeMonitoringRecordHandler;
    private final UpdateTreeMonitoringRecordHandler updateTreeMonitoringRecordHandler;
    private final DeleteTreeMonitoringRecordHandler deleteTreeMonitoringRecordHandler;
    private final GetLatestTreeMonitoringRecordsOfProjectHandler getLatestTreeMonitoringRecordsOfProjectHandler;

    public TreeMonitoringFacade(CreateTreeMonitoringRecordHandler createTreeMonitoringRecordHandler,
                                GetTreeMonitoringRecordsHandler getTreeMonitoringRecordsHandler,
                                GetTreeMonitoringRecordHandler getTreeMonitoringRecordHandler,
                                UpdateTreeMonitoringRecordHandler updateTreeMonitoringRecordHandler,
                                DeleteTreeMonitoringRecordHandler deleteTreeMonitoringRecordHandler,
                                GetLatestTreeMonitoringRecordsOfProjectHandler getLatestTreeMonitoringRecordsOfProjectHandler) {
        this.createTreeMonitoringRecordHandler = createTreeMonitoringRecordHandler;
        this.getTreeMonitoringRecordsHandler = getTreeMonitoringRecordsHandler;
        this.getTreeMonitoringRecordHandler = getTreeMonitoringRecordHandler;
        this.updateTreeMonitoringRecordHandler = updateTreeMonitoringRecordHandler;
        this.deleteTreeMonitoringRecordHandler = deleteTreeMonitoringRecordHandler;
        this.getLatestTreeMonitoringRecordsOfProjectHandler = getLatestTreeMonitoringRecordsOfProjectHandler;
    }

    public TreeMonitoringRecordResponse createRecordForTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                         TreeMonitoringRecordRequest request) {
        return createTreeMonitoringRecordHandler.handle(currentUserEmail, projectId, treeId, request, null);
    }

    /** Etap 36: tworzenie z identyfikatorem urzadzenia (ponowienie bez duplikatu). */
    public TreeMonitoringRecordResponse createRecordForTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                         TreeMonitoringRecordRequest request, String clientRequestId) {
        return createTreeMonitoringRecordHandler.handle(currentUserEmail, projectId, treeId, request, clientRequestId);
    }

    public List<TreeMonitoringRecordResponse> getRecordsOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId) {
        return getTreeMonitoringRecordsHandler.handle(currentUserEmail, projectId, treeId);
    }

    public TreeMonitoringRecordResponse getRecordOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                     Long recordId) {
        return getTreeMonitoringRecordHandler.handle(currentUserEmail, projectId, treeId, recordId);
    }

    public TreeMonitoringRecordResponse updateRecordOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId,
                                                                        Long recordId, TreeMonitoringRecordRequest request) {
        return updateTreeMonitoringRecordHandler.handle(currentUserEmail, projectId, treeId, recordId, request);
    }

    public void deleteRecordOfTreeOfCurrentUser(String currentUserEmail, Long projectId, Long treeId, Long recordId) {
        deleteTreeMonitoringRecordHandler.handle(currentUserEmail, projectId, treeId, recordId);
    }

    public List<TreeMonitoringRecordResponse> getLatestRecordsOfProjectOfCurrentUser(String currentUserEmail, Long projectId) {
        return getLatestTreeMonitoringRecordsOfProjectHandler.handle(currentUserEmail, projectId);
    }
}
