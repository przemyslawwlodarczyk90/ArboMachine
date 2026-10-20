package com.example.arbomachine.domain.treemarking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Body PUT .../trees/{treeId}/markings - KOMPLET oznaczen drzewa (pusta lista = brak oznaczen). */
public record TreeMarkingsRequest(
        @NotNull(message = "Marking codes are required")
        @Size(max = 64, message = "Too many marking codes")
        List<String> codes
) {}
