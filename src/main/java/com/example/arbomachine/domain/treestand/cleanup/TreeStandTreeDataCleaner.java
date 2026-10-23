package com.example.arbomachine.domain.treestand.cleanup;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.TreeDataCleaner;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Usuwane drzewo znika z grup drzew projektu (etap 44). Sama grupa zostaje - takze pusta (obszar grupy nie zalezy
 * od drzew).
 */
@Component
public class TreeStandTreeDataCleaner implements TreeDataCleaner {
    private final TreeStandRepository treeStandRepository;

    public TreeStandTreeDataCleaner(TreeStandRepository treeStandRepository) {
        this.treeStandRepository = treeStandRepository;
    }

    @Override
    public void deleteDataOf(Tree treeToDelete) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(treeToDelete.getProject().getId())
                .forEach(stand -> stand.removeTree(treeToDelete, now));
    }
}
