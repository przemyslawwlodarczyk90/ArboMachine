package com.example.arbomachine.domain.tree.service.treefacade;

import com.example.arbomachine.domain.tree.dto.TreeResponse;
import com.example.arbomachine.domain.tree.dto.UpdateTreeRequest;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.mapper.TreeMapper;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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

        String newInventoryNumber = TreeTextNormalizer.trimmedInventoryNumber(updateTreeRequest.inventoryNumber());
        String newSpeciesLatin = TreeTextNormalizer.trimmedOrNullWhenBlank(updateTreeRequest.speciesLatin());
        String newNotes = TreeTextNormalizer.trimmedOrNullWhenBlank(updateTreeRequest.notes());

        boolean anythingChanged = !Objects.equals(newInventoryNumber, treeInOwnedProject.getInventoryNumber())
                || !Objects.equals(newSpeciesLatin, treeInOwnedProject.getSpeciesLatin())
                || !Objects.equals(newNotes, treeInOwnedProject.getNotes());
        if (!anythingChanged) {
            return treeMapper.toTreeResponse(treeInOwnedProject);
        }

        treeInOwnedProject.setInventoryNumber(newInventoryNumber);
        treeInOwnedProject.setSpeciesLatin(newSpeciesLatin);
        treeInOwnedProject.setNotes(newNotes);
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak przy tworzeniu).
        treeInOwnedProject.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        return treeMapper.toTreeResponse(treeRepository.save(treeInOwnedProject));
    }
}
