package com.example.arbomachine.infrastructure.api.mapsymbol;

import com.example.arbomachine.domain.mapsymbol.dto.MapSymbolResponse;
import com.example.arbomachine.domain.mapsymbol.facade.MapSymbolFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Katalog symboli mapowych V1 (wymaga zalogowania - SecurityConfig). */
@RestController
@Tag(name = "Symbole mapowe", description = "Techniczny katalog symboli drzew i prac na mapie (V1)")
public class MapSymbolController {

    private final MapSymbolFacade mapSymbolFacade;

    public MapSymbolController(MapSymbolFacade mapSymbolFacade) {
        this.mapSymbolFacade = mapSymbolFacade;
    }

    @Operation(summary = "Dostepne symbole mapowe (kod, nazwa, ksztalt, kolory)")
    @GetMapping("/api/map-symbols")
    public ResponseEntity<List<MapSymbolResponse>> getMapSymbols() {
        return ResponseEntity.ok(mapSymbolFacade.getMapSymbols());
    }
}
