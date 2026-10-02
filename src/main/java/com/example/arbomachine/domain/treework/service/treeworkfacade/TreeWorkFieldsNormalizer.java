package com.example.arbomachine.domain.treework.service.treeworkfacade;

import com.example.arbomachine.domain.treework.entity.TreeWork;
import com.example.arbomachine.domain.treework.entity.TreeWorkPriority;
import com.example.arbomachine.domain.treework.entity.TreeWorkStatus;

import com.example.arbomachine.domain.treework.dto.TreeWorkFieldsRequest;

/**
 * Wspolne zasady normalizacji pol pracy - te same przy tworzeniu i edycji: nazwa przycieta, pusty opis zapisywany
 * jako null, brak statusu = RECOMMENDED (etap 28). Walidacja (wymagalnosc, dlugosci) jest wczesniej - na adnotacjach requestow.
 */
final class TreeWorkFieldsNormalizer {

    private TreeWorkFieldsNormalizer() {
    }

    /** Status brak -> RECOMMENDED; priorytet brak -> null; daty bez zmian (bez regul "nie w przeszlosci"). */
    static TreeWork.Fields normalizedFieldsOf(TreeWorkFieldsRequest treeWorkFieldsRequest) {
        String description = treeWorkFieldsRequest.description();
        return new TreeWork.Fields(
                treeWorkFieldsRequest.name().trim(),
                description == null || description.isBlank() ? null : description.trim(),
                treeWorkFieldsRequest.status() == null ? TreeWorkStatus.RECOMMENDED : TreeWorkStatus.valueOf(treeWorkFieldsRequest.status()),
                treeWorkFieldsRequest.priority() == null ? null : TreeWorkPriority.valueOf(treeWorkFieldsRequest.priority()),
                treeWorkFieldsRequest.plannedDate(),
                treeWorkFieldsRequest.completedDate());
    }
}
