package com.example.arbomachine.domain.treemarking.cleanup;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.TreeDataCleaner;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import org.springframework.stereotype.Component;

/** Usuwane drzewo traci oznaczenia (etap 44, w transakcji usuwania drzewa). */
@Component
public class TreeMarkingTreeDataCleaner implements TreeDataCleaner {
    private final TreeMarkingRepository treeMarkingRepository;

    public TreeMarkingTreeDataCleaner(TreeMarkingRepository treeMarkingRepository) {
        this.treeMarkingRepository = treeMarkingRepository;
    }

    @Override
    public void deleteDataOf(Tree treeToDelete) {
        treeMarkingRepository.deleteAll(treeMarkingRepository.findAllByTreeId(treeToDelete.getId()));
    }
}
