package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.entity.ClientRequestId;
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
import java.util.Optional;

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

    /** Etap 36: clientRequestId (UUID z urzadzenia, null = brak) - ponowienie zwraca juz utworzone drzewo zamiast drugiego. */
    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, CreateTreeRequest createTreeRequest, String clientRequestId) {
        Project ownedProject = findProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId);
        String normalizedClientRequestId = ClientRequestId.normalizedOrNull(clientRequestId);
        if (normalizedClientRequestId != null) {
            Optional<Tree> alreadyCreatedTree = treeRepository.findByProjectIdAndClientRequestId(ownedProject.getId(), normalizedClientRequestId);
            if (alreadyCreatedTree.isPresent()) return treeMapper.toTreeResponse(alreadyCreatedTree.get());
        }
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy projektach).
        Instant creationTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Tree newTree = Tree.createInProject(ownedProject,
                TreeEditableAttributesNormalizer.normalizedAttributesOf(createTreeRequest), creationTime);
        newTree.assignClientRequestId(normalizedClientRequestId);
        return treeMapper.toTreeResponse(treeRepository.save(newTree));
    }
}
