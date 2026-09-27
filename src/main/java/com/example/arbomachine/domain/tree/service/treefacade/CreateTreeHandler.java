package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.project.service.projectfacade.FindProjectOwnedByCurrentUserHandler;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Dodanie drzewa do WLASNEGO projektu - id projektu z URL, wlasnosc sprawdzana przed zapisem. */
@Component
public class CreateTreeHandler {
    private final FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;
    private final TreeMapper treeMapper;

    public CreateTreeHandler(FindProjectOwnedByCurrentUserHandler findProjectOwnedByCurrentUserHandler,
                             TreeRepository treeRepository, TreeMapper treeMapper) {
        this.findProjectOwnedByCurrentUserHandler = findProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, CreateTreeRequest createTreeRequest) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy projektach).
        Instant creationTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Tree newTree = Tree.createInProject(ownedProject,
                TreeTextNormalizer.trimmedInventoryNumber(createTreeRequest.inventoryNumber()),
                TreeTextNormalizer.trimmedOrNullWhenBlank(createTreeRequest.speciesLatin()),
                TreeTextNormalizer.trimmedOrNullWhenBlank(createTreeRequest.notes()),
                creationTime);
        return treeMapper.toTreeResponse(treeRepository.save(newTree));
    }
}
