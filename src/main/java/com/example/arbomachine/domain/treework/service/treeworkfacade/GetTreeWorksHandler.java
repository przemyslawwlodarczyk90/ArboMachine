package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/** Prace drzewa z WLASNEGO projektu, w kolejnosci dodawania. */
@Component
public class GetTreeWorksHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public GetTreeWorksHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                               TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    public List<TreeWorkResponse> handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        return treeWorkRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeInOwnedProject.getId()).stream()
                .map(treeWorkMapper::toTreeWorkResponse)
                .toList();
    }
}
