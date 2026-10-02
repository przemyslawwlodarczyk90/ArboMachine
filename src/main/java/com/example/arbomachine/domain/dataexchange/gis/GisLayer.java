package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;

import java.util.List;
import java.util.Optional;

/**
 * Warstwa pliku GIS po odczycie: nazwa, obiekty i uklad wspolrzednych. crs pusty = plik nie opisuje ukladu
 * (np. shapefile bez .prj, DXF) - uklad zostanie odgadniety z wartosci wspolrzednych (PolishGridCrs.guessFromCoordinates).
 * unsupportedCrsName - plik opisuje uklad, ktorego V1 nie obsluguje (import odrzucony z jego nazwa).
 */
public record GisLayer(String name, List<GisFeature> features, Optional<PolishGridCrs> crs, String unsupportedCrsName) {

    public static GisLayer of(String name, List<GisFeature> features, Optional<PolishGridCrs> crs) {
        return new GisLayer(name, features, crs, null);
    }

    public boolean hasPoints() {
        return features.stream().anyMatch(feature -> feature.geometry() instanceof GisFeature.Point);
    }

    public boolean hasPolygons() {
        return features.stream().anyMatch(feature -> feature.geometry() instanceof GisFeature.Polygon
                || feature.geometry() instanceof GisFeature.MultiPolygon);
    }
}
