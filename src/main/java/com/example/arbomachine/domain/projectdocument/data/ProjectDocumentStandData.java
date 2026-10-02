package com.example.arbomachine.domain.projectdocument.data;

import java.math.BigDecimal;
import java.util.List;

/** Grupa drzew w dokumencie (etap 29): nazwa, pole (przyblizone), numery drzew i uwagi. */
public record ProjectDocumentStandData(String name, BigDecimal areaSquareMeters, List<String> treeInventoryNumbers, String notes) {}
