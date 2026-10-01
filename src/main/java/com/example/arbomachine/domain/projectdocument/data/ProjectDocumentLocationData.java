package com.example.arbomachine.domain.projectdocument.data;

import java.math.BigDecimal;

/** Punkt drzewa w WGS84 (stopnie dziesietne) - jak TreeLocation. */
public record ProjectDocumentLocationData(BigDecimal latitude, BigDecimal longitude) {}
