package com.example.arbomachine.domain.treemonitoring.service.treemonitoringfacade;

import com.example.arbomachine.domain.treemonitoring.dto.TreeMonitoringRecordResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Pojedynczy wpis - tylko z drzewa z URL w WLASNYM projekcie. */
@Component
public class GetTreeMonitoringRecordHandler {
    private final FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler findRecordHandler;

    public GetTreeMonitoringRecordHandler(FindTreeMonitoringRecordOfTreeInProjectOwnedByCurrentUserHandler findRecordHandler) {
        this.findRecordHandler = findRecordHandler;
    }

    @Transactional(readOnly = true)
    public TreeMonitoringRecordResponse handle(String currentUserEmail, Long projectId, Long treeId, Long recordId) {
        return TreeMonitoringRecordMapper.toResponse(findRecordHandler.handle(currentUserEmail, projectId, treeId, recordId));
    }
}
