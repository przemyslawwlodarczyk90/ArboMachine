package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.tree.entity.ClientRequestId;
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
import java.util.Optional;

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

    /** Etap 36: clientRequestId (UUID z urzadzenia, null = brak) - ponowienie zwraca juz utworzona prace zamiast drugiej. */
    @Transactional
    public TreeWorkResponse handle(String currentUserEmail, Long projectId, Long treeId,
                                   CreateTreeWorkRequest createTreeWorkRequest, String clientRequestId) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);
        String normalizedClientRequestId = ClientRequestId.normalizedOrNull(clientRequestId);
        if (normalizedClientRequestId != null) {
            Optional<TreeWork> alreadyCreatedWork = treeWorkRepository.findByTreeIdAndClientRequestId(treeInOwnedProject.getId(), normalizedClientRequestId);
            if (alreadyCreatedWork.isPresent()) return treeWorkMapper.toTreeWorkResponse(alreadyCreatedWork.get());
        }
        TreeWork.Fields fields = TreeWorkFieldsNormalizer.normalizedFieldsOf(createTreeWorkRequest);
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy drzewach).
        TreeWork createdTreeWork = TreeWork.createForTree(treeInOwnedProject, fields,
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        createdTreeWork.assignClientRequestId(normalizedClientRequestId);
        return treeWorkMapper.toTreeWorkResponse(treeWorkRepository.save(createdTreeWork));
    }
}
