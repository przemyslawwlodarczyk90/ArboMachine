package com.example.arbomachine.domain.dataexchange.dto;

import java.math.BigDecimal;

/** Podglad poprawnego wiersza przed importem (pierwsze wiersze pliku) - wartosci po normalizacji. */
public record TreeImportPreviewRowResponse(
        int rowNumber,
        String inventoryNumber,
        String speciesPolish,
        String speciesLatin,
        Integer trunkCircumferenceCm,
        BigDecimal heightM,
        BigDecimal crownDiameterM,
        boolean hasLocation
) {}
