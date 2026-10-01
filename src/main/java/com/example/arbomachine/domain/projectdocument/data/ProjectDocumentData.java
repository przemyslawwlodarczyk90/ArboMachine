package com.example.arbomachine.domain.projectdocument.data;

import java.time.Instant;
import java.util.List;

/**
 * Jawny model wejsciowy generatora dokumentu projektu - proste dane skopiowane z encji przez
 * ProjectDocumentDataAssembler. Generator nie zna JPA ani repozytoriow, wiec zmiana modelu domenowego albo
 * wymiana generatora (np. na wzor programu prac konserwatorskich) nie wymaga przepisywania obu stron.
 * Celowo bez danych zleceniodawcy, autora i obiektu - nie sa jeszcze modelowane (placeholdery w szablonie).
 */
public record ProjectDocumentData(
        long projectId,
        String projectName,
        String projectDescription,
        Instant projectCreatedAt,
        Instant projectUpdatedAt,
        Instant generatedAt,
        List<ProjectDocumentTreeData> trees
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

    public long workCount() {
        return trees.stream().mapToLong(tree -> tree.works().size()).sum();
    }
}
