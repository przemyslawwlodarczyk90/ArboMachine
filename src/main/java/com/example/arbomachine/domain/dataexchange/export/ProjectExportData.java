package com.example.arbomachine.domain.dataexchange.export;

import java.time.Instant;
import java.util.List;

/** Dane projektu do eksportu - drzewa w kolejnosci dodania (z pracami, waloryzacja, kontrolami), grupy drzew, dane zlecenia. */
public record ProjectExportData(
        long projectId,
        String projectName,
        String projectDescription,
        Instant exportedAt,
        List<TreeExportData> trees,
        /** etap 29: dane zlecenia (null = nie podano) i grupy drzew */
        String clientName,
        String siteAddress,
        java.time.LocalDate orderDate,
        List<TreeStandExportData> stands
) {

    public long treesWithoutLocationCount() {
        return trees.stream().filter(tree -> !tree.hasLocation()).count();
    }
}
