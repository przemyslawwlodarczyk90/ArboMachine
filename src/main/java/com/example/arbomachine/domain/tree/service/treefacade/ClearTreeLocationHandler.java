package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Usuniecie lokalizacji drzewa w WLASNYM projekcie - "lokalizacja nie jest jeszcze ustalona" (null, nie 0,0).
 * Operacja idempotentna: drzewo bez lokalizacji zostaje bez zmian (updatedAt sie nie przesuwa).
 */
@Component
public class ClearTreeLocationHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;
    private final TreeMapper treeMapper;

    public ClearTreeLocationHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                    TreeRepository treeRepository, TreeMapper treeMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, Long treeId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        if (treeInOwnedProject.getLocation() == null) {
            return treeMapper.toTreeResponse(treeInOwnedProject);
        }

        treeInOwnedProject.clearLocation(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeMapper.toTreeResponse(treeRepository.save(treeInOwnedProject));
    }
}
