package com.example.arbomachine.domain.treemonitoring.cleanup;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.service.treefacade.TreeDataCleaner;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import org.springframework.stereotype.Component;

/** Usuwane drzewo traci kontrole stanu (etap 44, w transakcji usuwania drzewa). */
@Component
public class TreeMonitoringTreeDataCleaner implements TreeDataCleaner {
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;

    public TreeMonitoringTreeDataCleaner(TreeMonitoringRecordRepository treeMonitoringRecordRepository) {
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
    }

    @Override
    public void deleteDataOf(Tree treeToDelete) {
        treeMonitoringRecordRepository.deleteAll(
                treeMonitoringRecordRepository.findAllByTreeIdOrderByInspectionDateDescIdDesc(treeToDelete.getId()));
    }
}
