package com.example.arbomachine.domain.mapsymbol.facade;

import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolResponse;
import com.example.arbomachine.domain.mapsymbol.service.mapsymbolfacade.GetMapSymbolsHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/** Katalog symboli mapowych. Tylko deleguje do handlerow z service/mapsymbolfacade. */
@Component
public class MapSymbolFacade {
    private final GetMapSymbolsHandler getMapSymbolsHandler;

    public MapSymbolFacade(GetMapSymbolsHandler getMapSymbolsHandler) {
        this.getMapSymbolsHandler = getMapSymbolsHandler;
    }

    public List<MapSymbolResponse> getMapSymbols() {
        return getMapSymbolsHandler.handle();
    }
}
