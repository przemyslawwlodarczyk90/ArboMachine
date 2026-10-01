package com.example.arbomachine.domain.projectdocument.data;

import java.math.BigDecimal;
import java.util.List;

/** Drzewo w dokumencie: dane z karty drzewa (wartosci null = nie podano), lokalizacja, prace i zdjecia. */
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
        List<ProjectDocumentWorkData> works,
        List<ProjectDocumentPhotoData> photos
) {}
