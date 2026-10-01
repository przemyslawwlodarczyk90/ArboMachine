package com.example.arbomachine.domain.projectdocument.data;

import java.math.BigDecimal;

/** Punkt drzewa w WGS84 (stopnie dziesietne) z danymi pomiaru jak TreeLocation; source = nazwa TreeLocationSource albo null. */
public record ProjectDocumentLocationData(BigDecimal latitude, BigDecimal longitude, BigDecimal accuracyM, String source) {}
