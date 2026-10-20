package com.example.arbomachine.domain.treemarking.dto;

import java.util.List;

/** Oznaczenia jednego drzewa - kody w kolejnosci katalogu. */
public record TreeMarkingsResponse(
        Long treeId,
        List<String> codes
) {}
