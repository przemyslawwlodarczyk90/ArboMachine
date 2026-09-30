package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.FindTreeInProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.treework.dto.CreateTreeWorkRequest;
import com.example.arbomachine.domain.treework.dto.TreeWorkResponse;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.mapper.TreeWorkMapper;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Dodanie pracy do drzewa w WLASNYM projekcie (drzewo musi nalezec dokladnie do projektu z URL). */
@Component
public class CreateTreeWorkHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeWorkMapper treeWorkMapper;

    public CreateTreeWorkHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                                 TreeWorkRepository treeWorkRepository, TreeWorkMapper treeWorkMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeWorkRepository = treeWorkRepository;
        this.treeWorkMapper = treeWorkMapper;
    }

    @Transactional
    public TreeWorkResponse handle(String currentUserEmail, Long projectId, Long treeId,
                                   CreateTreeWorkRequest createTreeWorkRequest) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        TreeWorkFieldsNormalizer.NormalizedTreeWorkFields fields =
                TreeWorkFieldsNormalizer.normalizedFieldsOf(createTreeWorkRequest);
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy drzewach).
        TreeWork createdTreeWork = TreeWork.createForTree(treeInOwnedProject, fields.name(), fields.description(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeWorkMapper.toTreeWorkResponse(treeWorkRepository.save(createdTreeWork));
    }
}
