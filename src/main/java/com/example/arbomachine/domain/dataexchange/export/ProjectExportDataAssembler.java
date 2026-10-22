package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import com.example.arbomachine.domain.treestand.entity.TreeStand;
import com.example.arbomachine.domain.treestand.geometry.StandPolygon;
import com.example.arbomachine.infrastructure.repository.TreeMonitoringRecordRepository;
import com.example.arbomachine.infrastructure.repository.TreeStandRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.arbomachine.infrastructure.repository.TreeValuationRepository;
import com.example.arbomachine.infrastructure.repository.TreeMarkingRepository;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.infrastructure.repository.TreeWorkRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sklada ProjectExportData z WLASNEGO projektu (wlasnosc sprawdza wczesniej handler): szesc zapytan - drzewa,
 * prace, waloryzacje, kontrole stanu, grupy drzew i oznaczenia calego projektu - laczone w pamieci (bez N+1). Zdjecia nie sa eksportowane (V1).
 */
@Component
public class ProjectExportDataAssembler {
    private final TreeRepository treeRepository;
    private final TreeWorkRepository treeWorkRepository;
    private final TreeValuationRepository treeValuationRepository;
    private final TreeMonitoringRecordRepository treeMonitoringRecordRepository;
    private final TreeStandRepository treeStandRepository;
    private final TreeMarkingRepository treeMarkingRepository;
    private final TreeMarkingCatalog treeMarkingCatalog;
    private final ObjectMapper objectMapper;

    public ProjectExportDataAssembler(TreeRepository treeRepository, TreeWorkRepository treeWorkRepository,
                                      TreeValuationRepository treeValuationRepository,
                                      TreeMonitoringRecordRepository treeMonitoringRecordRepository,
                                      TreeStandRepository treeStandRepository, TreeMarkingRepository treeMarkingRepository,
                                      TreeMarkingCatalog treeMarkingCatalog, ObjectMapper objectMapper) {
        this.treeRepository = treeRepository;
        this.treeWorkRepository = treeWorkRepository;
        this.treeValuationRepository = treeValuationRepository;
        this.treeMonitoringRecordRepository = treeMonitoringRecordRepository;
        this.treeStandRepository = treeStandRepository;
        this.treeMarkingRepository = treeMarkingRepository;
        this.treeMarkingCatalog = treeMarkingCatalog;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ProjectExportData assemble(Project ownedProject, Instant exportedAt) {
        Map<Long, List<TreeWorkExportData>> worksByTreeId = treeWorkRepository
                .findAllByTreeProjectIdOrderByTreeIdAscCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(work -> work.getTree().getId(),
                        Collectors.mapping(ProjectExportDataAssembler::workDataOf, Collectors.toList())));
        Map<Long, TreeValuation> valuationByTreeId = treeValuationRepository.findAllByTreeProjectId(ownedProject.getId()).stream()
                .collect(Collectors.toMap(valuation -> valuation.getTree().getId(), Function.identity()));
        Map<Long, List<TreeMonitoringExportData>> monitoringByTreeId = treeMonitoringRecordRepository
                .findAllOfProjectNewestFirst(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(record -> record.getTree().getId(), Collectors.mapping(
                        record -> new TreeMonitoringExportData(record.getId(), record.getInspectionDate(),
                                record.getHealthStatus() == null ? null : record.getHealthStatus().name(), record.getNotes()),
                        Collectors.toList())));
        List<TreeStand> stands = treeStandRepository.findAllByProjectIdOrderByNameAscIdAsc(ownedProject.getId());
        Map<Long, List<String>> standNamesByTreeId = new java.util.HashMap<>();
        for (TreeStand stand : stands) {
            stand.getTrees().forEach(tree -> standNamesByTreeId.computeIfAbsent(tree.getId(), id -> new java.util.ArrayList<>()).add(stand.getName()));
        }
        Map<Long, List<String>> markingCodesByTreeId = treeMarkingRepository.findTreeIdsAndCodesByProjectId(ownedProject.getId()).stream()
                .collect(Collectors.groupingBy(TreeMarkingRepository.TreeIdAndCode::getTreeId,
                        Collectors.mapping(TreeMarkingRepository.TreeIdAndCode::getCode, Collectors.toList())));
        List<TreeExportData> trees = treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .map(tree -> treeDataOf(tree, worksByTreeId.getOrDefault(tree.getId(), List.of()), valuationByTreeId.get(tree.getId()),
                        monitoringByTreeId.getOrDefault(tree.getId(), List.of()), standNamesByTreeId.getOrDefault(tree.getId(), List.of()),
                        treeMarkingCatalog.displayNamesInCatalogOrder(markingCodesByTreeId.getOrDefault(tree.getId(), List.of())),
                        treeMarkingCatalog.inCatalogOrder(markingCodesByTreeId.getOrDefault(tree.getId(), List.of()))))
                .toList();
        return new ProjectExportData(ownedProject.getId(), ownedProject.getName(), ownedProject.getDescription(),
                exportedAt, trees, ownedProject.getClientName(), ownedProject.getSiteAddress(), ownedProject.getOrderDate(),
                stands.stream().map(this::standDataOf).toList());
    }

    private static TreeWorkExportData workDataOf(TreeWork work) {
        return new TreeWorkExportData(work.getId(), work.getName(), work.getDescription(), work.getMapSymbolCode(),
                work.getStatus().name(), work.getPriority() == null ? null : work.getPriority().name(),
                work.getPlannedDate(), work.getCompletedDate());
    }

    private static TreeValuationExportData valuationDataOf(TreeValuation valuation) {
        if (valuation == null) return null;
        return new TreeValuationExportData(valuation.getOverallCategory() == null ? null : valuation.getOverallCategory().name(),
                valuation.getAgeValue(), valuation.getNaturalValue(), valuation.getEcologicalValue(), valuation.getCulturalValue(),
                valuation.getLandscapeValue(), valuation.getJustification());
    }

    private TreeStandExportData standDataOf(TreeStand stand) {
        return new TreeStandExportData(stand.getId(), stand.getName(), stand.getNotes(), stand.getSource().name(),
                stand.getAreaSquareMeters(), StandPolygon.fromGeoJsonText(stand.getGeometryGeoJson(), objectMapper).rings(),
                stand.getTrees().stream().sorted(java.util.Comparator.comparing(Tree::getId)).map(Tree::getInventoryNumber).toList());
    }

    private static TreeExportData treeDataOf(Tree tree, List<TreeWorkExportData> works, TreeValuation valuation,
                                             List<TreeMonitoringExportData> monitoringRecords, List<String> standNames,
                                             List<String> markingNames, List<String> markingCodes) {
        return new TreeExportData(tree.getId(), tree.getInventoryNumber(), tree.getSpeciesPolish(), tree.getSpeciesLatin(),
                tree.getTrunkCircumferenceCm(), tree.getHeightM(), tree.getCrownDiameterM(), tree.getNotes(),
                tree.getConditionNotes(), tree.getHazardNotes(),
                tree.getLocation() == null ? null : tree.getLocation().getLatitude(),
                tree.getLocation() == null ? null : tree.getLocation().getLongitude(),
                tree.getLocation() == null ? null : tree.getLocation().getAccuracyM(),
                tree.getLocation() == null ? null : tree.getLocation().getAltitudeM(),
                tree.getLocation() == null || tree.getLocation().getSource() == null ? null : tree.getLocation().getSource().name(),
                tree.getMapSymbolCode(), works, valuationDataOf(valuation), monitoringRecords, standNames, markingNames, markingCodes);
    }
}
