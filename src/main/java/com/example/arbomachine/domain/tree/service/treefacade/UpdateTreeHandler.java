package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.dto.UpdateTreeRequest;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.entity.TreeEditableAttributes;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Edycja drzewa w WLASNYM projekcie. Drzewo musi nalezec dokladnie do projektu z URL - drzewa z innego projektu
 * (nawet tego samego uzytkownika) nie da sie zmienic przez zly projectId. Projekt drzewa nigdy sie nie zmienia.
 * updatedAt przesuwa sie tylko przy rzeczywistej zmianie (ten sam mechanizm czasu co przy tworzeniu).
 */
@Component
public class UpdateTreeHandler {
    private final FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler;
    private final TreeRepository treeRepository;
    private final TreeMapper treeMapper;

    public UpdateTreeHandler(FindTreeInProjectOwnedByCurrentUserHandler findTreeInProjectOwnedByCurrentUserHandler,
                             TreeRepository treeRepository, TreeMapper treeMapper) {
        this.findTreeInProjectOwnedByCurrentUserHandler = findTreeInProjectOwnedByCurrentUserHandler;
        this.treeRepository = treeRepository;
        this.treeMapper = treeMapper;
    }

    @Transactional
    public TreeResponse handle(String currentUserEmail, Long projectId, Long treeId, UpdateTreeRequest updateTreeRequest) {
        Tree treeInOwnedProject = findTreeInProjectOwnedByCurrentUserHandler.handle(currentUserEmail, projectId, treeId);

        TreeEditableAttributes newAttributes = TreeEditableAttributesNormalizer.normalizedAttributesOf(updateTreeRequest);
        if (newAttributes.equals(treeInOwnedProject.editableAttributes())) {
            return treeMapper.toTreeResponse(treeInOwnedProject);
        }

        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy tworzeniu).
        treeInOwnedProject.replaceEditableAttributes(newAttributes, Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeMapper.toTreeResponse(treeRepository.save(treeInOwnedProject));
    }
}
