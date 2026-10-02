package com.example.arbomachine.domain.treestand.service.treestandfacade;

import com.example.arbomachine.domain.treestand.dto.TreeStandResponse;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Pojedyncza grupa drzew wlasnego projektu. */
@Component
public class GetTreeStandHandler {
    private final FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler;
    private final TreeStandMapper treeStandMapper;

    public GetTreeStandHandler(FindTreeStandOfProjectOwnedByCurrentUserHandler findTreeStandHandler, TreeStandMapper treeStandMapper) {
        this.findTreeStandHandler = findTreeStandHandler;
        this.treeStandMapper = treeStandMapper;
    }

    @Transactional(readOnly = true)
    public TreeStandResponse handle(String currentUserEmail, Long projectId, Long standId) {
        return treeStandMapper.toResponse(findTreeStandHandler.handle(currentUserEmail, projectId, standId));
    }
}
