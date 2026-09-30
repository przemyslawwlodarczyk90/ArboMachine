package com.example.arbomachine.domain.mapsymbol.dto;

import com.example.arbomachine.domain.mapsymbol.catalog.MapSymbolCatalog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body PUT .../map-symbol (drzewa albo pracy). Tylko kod symbolu - identyfikatory pochodza z URL i tokenu.
 * Czy kod istnieje w katalogu, sprawdza MapSymbolCatalog w handlerze.
 */
public record MapSymbolRequest(
        @NotBlank(message = "Map symbol code is mandatory")
        @Size(max = MapSymbolCatalog.MAXIMUM_SYMBOL_CODE_LENGTH, message = "Map symbol code must have at most 64 characters")
        String symbolCode
) {}
