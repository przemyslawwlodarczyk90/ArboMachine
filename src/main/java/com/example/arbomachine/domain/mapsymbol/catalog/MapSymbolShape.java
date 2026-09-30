package com.example.arbomachine.domain.mapsymbol.catalog;

/**
 * Ksztalt symbolu mapowego V1 - techniczny opis wygladu, ktory frontend rysuje w OpenLayers (RegularShape/Circle).
 * Nie niesie znaczenia arborystycznego. Wartosci ida do API jako tekst, wiec nowy ksztalt nie wymaga migracji DB.
 */
public enum MapSymbolShape {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    CROSS,
    DIAMOND,
    X
}
