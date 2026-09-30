package com.example.arbomachine.domain.mapsymbol.service.mapsymbolfacade;

import com.example.arbomachine.domain.mapsymbol.catalog.MapSymbolCatalog;
import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/** Katalog symboli V1 w stalej kolejnosci. */
@Component
public class GetMapSymbolsHandler {
    private final MapSymbolCatalog mapSymbolCatalog;

    public GetMapSymbolsHandler(MapSymbolCatalog mapSymbolCatalog) {
        this.mapSymbolCatalog = mapSymbolCatalog;
    }

    public List<MapSymbolResponse> handle() {
        return mapSymbolCatalog.allSymbols().stream()
                .map(symbol -> new MapSymbolResponse(symbol.code(), symbol.displayName(), symbol.shape().name(),
                        symbol.fillColor(), symbol.strokeColor()))
                .toList();
    }
}
