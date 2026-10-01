package com.example.arbomachine.domain.dataexchange.crs;

import java.math.BigDecimal;

/**
 * Miejsce na transformacje ukladu wspolrzednych w eksporcie GIS - domena (TreeLocation) zawsze przechowuje WGS84.
 * V1: tylko EPSG:4326 (Wgs84CoordinateTransformer, bez zmian wartosci). Gdy projekty beda wymagaly PL-2000 / PL-1992,
 * dojdzie tu implementacja (np. proj4j) i wybor ukladu przy eksporcie - bez zmian w encjach (docs/DO_DOPRECYZOWANIA.md).
 */
public interface CoordinateTransformer {

    /** Kod ukladu wyniku, np. "EPSG:4326". */
    String targetCrsCode();

    /** WGS84 (szerokosc, dlugosc) -> wspolrzedne x/y w ukladzie docelowym (dla EPSG:4326: x = dlugosc, y = szerokosc). */
    ExportCoordinates transform(BigDecimal latitude, BigDecimal longitude);

    record ExportCoordinates(BigDecimal x, BigDecimal y) {}
}
