package com.example.arbomachine.domain.treevaluation.cleanup;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.TreeDataCleaner;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import org.springframework.stereotype.Component;

/** Usuwane drzewo traci waloryzacje (etap 44, w transakcji usuwania drzewa). */
@Component
public class TreeValuationTreeDataCleaner implements TreeDataCleaner {
    private final TreeValuationRepository treeValuationRepository;

    public TreeValuationTreeDataCleaner(TreeValuationRepository treeValuationRepository) {
        this.treeValuationRepository = treeValuationRepository;
    }

    @Override
    public void deleteDataOf(Tree treeToDelete) {
        treeValuationRepository.findByTreeId(treeToDelete.getId()).ifPresent(treeValuationRepository::delete);
    }
}
