package com.example.arbomachine.domain.treevaluation.dto;

import com.example.arbomachine.domain.treevaluation.entity.TreeValuation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body PUT /api/projects/{projectId}/trees/{treeId}/valuation - komplet oceny (pominiete pole = brak wartosci).
 * Wszystkie pola opcjonalne, ale cala ocena nie moze byc pusta (do usuniecia sluzy DELETE). Bez id drzewa, projektu
 * i wlasciciela - pochodza z URL i JWT. overallCategory: LOW / MEDIUM / HIGH / VERY_HIGH albo brak.
 */
public record TreeValuationRequest(
        @Pattern(regexp = "LOW|MEDIUM|HIGH|VERY_HIGH", message = "Unsupported valuation category")
        String overallCategory,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String ageValue,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String naturalValue,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String ecologicalValue,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String culturalValue,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String landscapeValue,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String justification,

        @Size(max = TreeValuation.MAXIMUM_TEXT_LENGTH, message = "Valuation text must have at most 5000 characters")
        String notes
) {}
