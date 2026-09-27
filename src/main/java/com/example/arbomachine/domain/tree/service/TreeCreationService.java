package com.example.arbomachine.domain.tree.service;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.dto.CreateTreeRequest;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Dodanie drzewa do projektu, ktory wywolujacy juz pobral z kontrola wlasnosci.
 * Normalizuje tekst: przycina spacje, a puste pola opcjonalne zapisuje jako null.
 */
@Service
public class TreeCreationService {
    private final TreeRepository treeRepository;

    public TreeCreationService(TreeRepository treeRepository) {
        this.treeRepository = treeRepository;
    }

    public Tree createTreeInProject(Project ownedProject, CreateTreeRequest createTreeRequest) {
        // Precyzja do mikrosekund = precyzja kolumny timestamp w PostgreSQL (jak w ProjectCreationService).
        Instant creationTime = Instant.now().truncatedTo(ChronoUnit.MICROS);
        Tree newTree = Tree.createInProject(ownedProject,
                createTreeRequest.inventoryNumber().trim(),
                trimmedOrNullWhenBlank(createTreeRequest.speciesLatin()),
                trimmedOrNullWhenBlank(createTreeRequest.notes()),
                creationTime);
        return treeRepository.save(newTree);
    }

    private static String trimmedOrNullWhenBlank(String optionalText) {
        return optionalText == null || optionalText.isBlank() ? null : optionalText.trim();
    }
}
