package com.example.arbomachine.domain.treework.cleanup;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.TreeDataCleaner;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;

/** Usuwane drzewo traci zalecenia i prace (etap 44, w transakcji usuwania drzewa). */
@Component
public class TreeWorkTreeDataCleaner implements TreeDataCleaner {
    private final TreeWorkRepository treeWorkRepository;

    public TreeWorkTreeDataCleaner(TreeWorkRepository treeWorkRepository) {
        this.treeWorkRepository = treeWorkRepository;
    }

    @Override
    public void deleteDataOf(Tree treeToDelete) {
        treeWorkRepository.deleteAll(treeWorkRepository.findAllByTreeIdOrderByCreatedAtAscIdAsc(treeToDelete.getId()));
    }
}
