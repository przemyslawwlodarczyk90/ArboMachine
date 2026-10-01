package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Sklada ProjectExportData z WLASNEGO projektu (wlasnosc sprawdza wczesniej handler): dwa zapytania - drzewa
 * i prace calego projektu - laczone w pamieci (bez N+1). Zdjecia nie sa eksportowane (V1).
 */
@Component
public class ProjectExportDataAssembler {
    private final TreeRepository treeRepository;
    private final TreeWorkRepository treeWorkRepository;

    public ProjectExportDataAssembler(TreeRepository treeRepository, TreeWorkRepository treeWorkRepository) {
        this.treeRepository = treeRepository;
        this.treeWorkRepository = treeWorkRepository;
    }

    @Transactional(readOnly = true)
    public ProjectExportData assemble(Project ownedProject, Instant exportedAt) {
        Map<Long, List<TreeWorkExportData>> worksByTreeId = treeWorkRepository
                .findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(work -> work.getTree().getId(),
                        Collectors.mapping(ProjectExportDataAssembler::workDataOf, Collectors.toList())));
        List<TreeExportData> trees = treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .map(tree -> treeDataOf(tree, worksByTreeId.getOrDefault(tree.getId(), List.of())))
                .toList();
        return new ProjectExportData(ownedProject.getId(), ownedProject.getName(), ownedProject.getDescription(),
                exportedAt, trees);
    }

    private static TreeWorkExportData workDataOf(TreeWork work) {
        return new TreeWorkExportData(work.getName(), work.getDescription(), work.getMapSymbolCode());
    }

    private static TreeExportData treeDataOf(Tree tree, List<TreeWorkExportData> works) {
        return new TreeExportData(tree.getId(), tree.getInventoryNumber(), tree.getSpeciesPolish(), tree.getSpeciesLatin(),
                tree.getTrunkCircumferenceCm(), tree.getHeightM(), tree.getCrownDiameterM(), tree.getNotes(),
                tree.getConditionNotes(), tree.getHazardNotes(),
                tree.getLocation() == null ? null : tree.getLocation().getLatitude(),
                tree.getLocation() == null ? null : tree.getLocation().getLongitude(),
                tree.getLocation() == null ? null : tree.getLocation().getAccuracyM(),
                tree.getLocation() == null ? null : tree.getLocation().getAltitudeM(),
                tree.getLocation() == null || tree.getLocation().getSource() == null ? null : tree.getLocation().getSource().name(),
                tree.getMapSymbolCode(), works);
    }
}
