package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

/** Pojedyncze drzewo z wlasnego projektu; drzewo z innego projektu -> 404. */
@Component
public class GetTreeHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeMapper treeMapper;

    public GetTreeHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                          TreeMapper treeMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, Long treeId) {
        return treeMapper.toTreeResponse(findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId));
    }
}
