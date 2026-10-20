package com.example.arbomachine.domain.dataexchange.gis;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Obiekt pliku GIS/CAD po odczycie (etap 29), niezalezny od formatu: numer obiektu w pliku (od 1), geometria
 * we wspolrzednych PLIKU (x = easting / dlugosc, y = northing / szerokosc) i atrybuty jako tekst (nazwa kolumny -> wartosc).
 */
public record GisFeature(int number, Geometry geometry, Map<String, String> attributes) {

    /** Geometria obiektu; null w GisFeature = obiekt bez geometrii. */
    public sealed interface Geometry permits Point, LineString, MultiLineString, Polygon, MultiPolygon, Unsupported {}

    public record Point(double x, double y) implements Geometry {}

    /** Linia (etap 31 - warstwy referencyjne: granice, drogi, sieci): punkty [x, y] w kolejnosci. */
    public record LineString(List<double[]> points) implements Geometry {}

    public record MultiLineString(List<LineString> lines) implements Geometry {}

    /** Pierscienie [x, y]: pierwszy = obrys, kolejne = dziury. */
    public record Polygon(List<List<double[]>> rings) implements Geometry {}

    public record MultiPolygon(List<Polygon> polygons) implements Geometry {}

    /** Inny typ geometrii (wielopunkt, kolekcja...) - tylko nazwa do komunikatu. */
    public record Unsupported(String typeName) implements Geometry {}

    /** Pierwszy wierzcholek geometrii (do odgadniecia ukladu wspolrzednych); pusty dla pustej / nieobslugiwanej. */
    public static Optional<double[]> firstPositionOf(Geometry geometry) {
        return switch (geometry) {
            case Point point -> Optional.of(new double[]{point.x(), point.y()});
            case LineString line -> line.points().stream().findFirst();
            case MultiLineString lines -> lines.lines().stream().map(GisFeature::firstPositionOf).flatMap(Optional::stream).findFirst();
            case Polygon polygon -> polygon.rings().stream().flatMap(List::stream).findFirst();
            case MultiPolygon polygons -> polygons.polygons().stream().map(GisFeature::firstPositionOf).flatMap(Optional::stream).findFirst();
            case Unsupported ignored -> Optional.empty();
            case null -> Optional.empty();
        };
    }
}
