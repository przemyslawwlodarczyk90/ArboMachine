package com.example.arbomachine.domain.projectdocument.data;

import java.time.Instant;
import java.util.List;

/**
 * Jawny model wejsciowy generatora dokumentu projektu - proste dane skopiowane z encji przez
 * ProjectDocumentDataAssembler. Generator nie zna JPA ani repozytoriow, wiec zmiana modelu domenowego albo
 * wymiana generatora (np. na wzor programu prac konserwatorskich) nie wymaga przepisywania obu stron.
 * Dane zlecenia (zleceniodawca, adres obiektu, data zlecenia) od etapu 29; autor opracowania z profilu wlasciciela
 * projektu od etapu 42 (null = nie uzupelniono -> placeholder w szablonie).
 */
public record ProjectDocumentData(
        long projectId,
        String projectName,
        String projectDescription,
        String clientName,
        String siteAddress,
        java.time.LocalDate orderDate,
        Instant projectCreatedAt,
        Instant projectUpdatedAt,
        Instant generatedAt,
        List<ProjectDocumentTreeData> trees,
        /** etap 29: grupy drzew projektu (wg nazwy) */
        List<ProjectDocumentStandData> stands,
        /** etap 42: dane autora opracowania albo null */
        ProjectDocumentAuthorData author
) {

    public int treeCount() {
        return trees.size();
    }

    public long treeWithLocationCount() {
        return trees.stream().filter(tree -> tree.location() != null).count();
    }

    public long photoCount() {
        return trees.stream().mapToLong(tree -> tree.photos().size()).sum();
    }

    /** Drzewa z zapisana waloryzacja (oceną arborysty). */
    public long valuedTreeCount() {
        return trees.stream().filter(tree -> tree.valuation() != null).count();
    }

    /** Prace do wykonania (zalecone albo zaplanowane) - etap 28. */
    public long openWorkCount() {
        return trees.stream().flatMap(tree -> tree.works().stream()).filter(ProjectDocumentWorkData::isOpen).count();
    }

    public long completedWorkCount() {
        return trees.stream().flatMap(tree -> tree.works().stream()).filter(work -> "COMPLETED".equals(work.status())).count();
    }

    /** Drzewa z co najmniej jedna kontrola stanu (etap 29). */
    public long monitoredTreeCount() {
        return trees.stream().filter(tree -> !tree.monitoringRecords().isEmpty()).count();
    }

    /** Drzewa, ktorych OSTATNIA kontrola ma ocene "zly" (POOR). */
    public long poorHealthTreeCount() {
        return trees.stream().map(ProjectDocumentTreeData::latestMonitoringRecord)
                .filter(record -> record != null && "POOR".equals(record.healthStatus())).count();
    }

    public long workCount() {
        return trees.stream().mapToLong(tree -> tree.works().size()).sum();
    }
}
