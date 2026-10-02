package com.example.arbomachine.domain.projectdocument.data;

import java.math.BigDecimal;
import java.util.List;

/** Drzewo w dokumencie: dane z karty drzewa (wartosci null = nie podano), lokalizacja, waloryzacja (null = brak), prace, zdjecia i kontrole stanu. */
public record ProjectDocumentTreeData(
        long treeId,
        String inventoryNumber,
        String speciesPolish,
        String speciesLatin,
        Integer trunkCircumferenceCm,
        BigDecimal heightM,
        BigDecimal crownDiameterM,
        String notes,
        String conditionNotes,
        String hazardNotes,
        ProjectDocumentLocationData location,
        ProjectDocumentValuationData valuation,
        List<ProjectDocumentWorkData> works,
        List<ProjectDocumentPhotoData> photos,
        /** etap 29: kontrole stanu, najnowsza pierwsza (pusta lista = brak monitoringu) */
        List<ProjectDocumentMonitoringData> monitoringRecords
) {

    public ProjectDocumentMonitoringData latestMonitoringRecord() {
        return monitoringRecords.isEmpty() ? null : monitoringRecords.getFirst();
    }
}
