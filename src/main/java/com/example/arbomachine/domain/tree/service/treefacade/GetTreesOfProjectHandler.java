package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;

/** Drzewa wlasnego projektu w kolejnosci dodawania - najpierw kontrola wlasnosci projektu. */
@Component
public class GetTreesOfProjectHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;
    private final TreeMapper treeMapper;

    public GetTreesOfProjectHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                                    TreeRepository treeRepository, TreeMapper treeMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public List<TreeResponse> handle(String currentUserEmail, Long projectId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        return treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .map(treeMapper::toTreeResponse)
                .toList();
    }
}
