package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import org.springframework.stereotype.Component;

/** Pojedyncza praca - tylko z drzewa z URL w WLASNYM projekcie. */
@Component
public class GetTreeWorkHandler {
    private final FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkMapper treeWorkMapper;

    public GetTreeWorkHandler(
            FindTreeWorkOfTreeInProjectOwnedByCurrentUserHandler findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler,
            TreeWorkMapper treeWorkMapper) {
        this.findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler = findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkMapper = treeWorkMapper;
    }

    public TreeWorkResponse handle(String currentUserEmail, Long projectId, Long treeId, Long treeWorkId) {
        return treeWorkMapper.toTreeWorkResponse(
                findTreeWorkOfTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId, treeWorkId));
    }
}
