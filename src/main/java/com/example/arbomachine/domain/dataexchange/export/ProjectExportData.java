package com.example.arbomachine.domain.dataexchange.export;

import java.time.Instant;
import java.util.List;

/** Dane projektu do eksportu - drzewa w kolejnosci dodania, z pracami. */
public record ProjectExportData(
        long projectId,
        String projectName,
        String projectDescription,
        Instant exportedAt,
        List<TreeExportData> trees
) {

    public long treesWithoutLocationCount() {
        return trees.stream().filter(tree -> !tree.hasLocation()).count();
    }
}
