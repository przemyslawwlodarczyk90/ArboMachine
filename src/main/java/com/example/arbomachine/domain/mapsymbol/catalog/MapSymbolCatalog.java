package com.example.arbomachine.domain.mapsymbol.catalog;

import com.example.arbomachine.domain.mapsymbol.exception.UnsupportedMapSymbolException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Katalog symboli mapowych V1 - w kodzie, nie w bazie (zmiana katalogu nie wymaga migracji). Backend jest zrodlem
 * prawdy dla dozwolonych kodow: Tree i TreeWork moga zapisac tylko kod z tej listy.
 * <p>
 * To TECHNICZNE style (ksztalt + kolor), a nie typy prac arborystycznych: "RED_CROSS" nie oznacza wycinki.
 * Znaczenie nadaje uzytkownik, przypisujac symbol konkretnej pracy; legenda bierze tekst z nazwy pracy.
 * Wlasciwa biblioteka symboli arborysty (SVG, ZWCAD / aplikacja terenowa) - docs/DO_DOPRECYZOWANIA.md.
 * Waski kontrakt dla modulow tree i treework (walidacja kodu przy zapisie).
 */
@Component
public class MapSymbolCatalog {

    /** Techniczny limit kodu = dlugosc kolumny map_symbol_code. */
    public static final int MAXIMUM_SYMBOL_CODE_LENGTH = 64;

    private static final String WHITE = "#FFFFFF";

    // Zielone kolo celowo jasniejsze niz neutralny marker drzewa bez symbolu (#2f6b3c) - inaczej byly nie do odroznienia.

    private static final List<MapSymbolDefinition> SYMBOLS = List.of(
            new MapSymbolDefinition("GREEN_CIRCLE", "Zielone koło", MapSymbolShape.CIRCLE, "#7CB342", WHITE),
            new MapSymbolDefinition("BLUE_SQUARE", "Niebieski kwadrat", MapSymbolShape.SQUARE, "#1565C0", WHITE),
            new MapSymbolDefinition("ORANGE_TRIANGLE", "Pomarańczowy trójkąt", MapSymbolShape.TRIANGLE, "#EF6C00", WHITE),
            new MapSymbolDefinition("RED_CROSS", "Czerwony krzyż", MapSymbolShape.CROSS, "#C62828", WHITE),
            new MapSymbolDefinition("PURPLE_DIAMOND", "Fioletowy romb", MapSymbolShape.DIAMOND, "#6A1B9A", WHITE),
            new MapSymbolDefinition("BLACK_X", "Czarny iks", MapSymbolShape.X, "#212121", WHITE)
    );

    private static final Map<String, MapSymbolDefinition> SYMBOL_BY_CODE = SYMBOLS.stream()
            .collect(Collectors.toUnmodifiableMap(MapSymbolDefinition::code, Function.identity()));

    /** Wszystkie symbole w stalej kolejnosci (kolejnosc wyboru w UI). */
    public List<MapSymbolDefinition> allSymbols() {
        return SYMBOLS;
    }

    /**
     * Kod z requestu po przycieciu, jesli jest w katalogu; inaczej UnsupportedMapSymbolException (HTTP 400).
     * Dzieki temu do bazy nie trafia dowolny tekst (CSS, SVG, sciezka, URL).
     */
    public String requireSupportedCode(String requestedSymbolCode) {
        String trimmedSymbolCode = requestedSymbolCode == null ? "" : requestedSymbolCode.trim();
        if (!SYMBOL_BY_CODE.containsKey(trimmedSymbolCode)) {
            throw new UnsupportedMapSymbolException("Unsupported map symbol");
        }
        return trimmedSymbolCode;
    }
}
