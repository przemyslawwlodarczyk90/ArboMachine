package com.example.arbomachine.domain.treevaluation.service.treevaluationfacade;

import com.example.arbomachine.domain.treevaluation.dto.TreeValuationRequest;
import com.example.arbomachine.domain.treevaluation.dto.TreeValuationResponse;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import com.example.arbomachine.domain.treevaluation.entity.TreeValuationCategory;

/** Request -> znormalizowana ocena (przyciety tekst, puste = null) i encja -> odpowiedz API. Pomocnik handlerow fasady. */
final class TreeValuationMapper {

    private TreeValuationMapper() {
    }

    static TreeValuation.Assessment assessmentOf(TreeValuationRequest request) {
        return new TreeValuation.Assessment(
                request.overallCategory() == null ? null : TreeValuationCategory.valueOf(request.overallCategory()),
                trimmedOrNull(request.ageValue()), trimmedOrNull(request.naturalValue()),
                trimmedOrNull(request.ecologicalValue()), trimmedOrNull(request.culturalValue()),
                trimmedOrNull(request.landscapeValue()), trimmedOrNull(request.justification()),
                trimmedOrNull(request.notes()));
    }

    static TreeValuationResponse toResponse(TreeValuation valuation) {
        return new TreeValuationResponse(valuation.getTree().getId(), true,
                valuation.getOverallCategory() == null ? null : valuation.getOverallCategory().name(),
                valuation.getAgeValue(), valuation.getNaturalValue(), valuation.getEcologicalValue(),
                valuation.getCulturalValue(), valuation.getLandscapeValue(), valuation.getJustification(),
                valuation.getNotes(), valuation.getCreatedAt(), valuation.getUpdatedAt());
    }

    private static String trimmedOrNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
