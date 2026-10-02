package com.example.arbomachine.domain.dataexchange.gis;

import java.util.List;
import java.util.Map;

/**
 * Obiekt pliku GIS/CAD po odczycie (etap 29), niezalezny od formatu: numer obiektu w pliku (od 1), geometria
 * we wspolrzednych PLIKU (x = easting / dlugosc, y = northing / szerokosc) i atrybuty jako tekst (nazwa kolumny -> wartosc).
 */
public record GisFeature(int number, Geometry geometry, Map<String, String> attributes) {

    /** Geometria obiektu; null w GisFeature = obiekt bez geometrii. */
    public sealed interface Geometry permits Point, Polygon, MultiPolygon, Unsupported {}

    public record Point(double x, double y) implements Geometry {}

    /** Pierscienie [x, y]: pierwszy = obrys, kolejne = dziury. */
    public record Polygon(List<List<double[]>> rings) implements Geometry {}

    public record MultiPolygon(List<Polygon> polygons) implements Geometry {}

    /** Inny typ geometrii (linia, wielopunkt...) - tylko nazwa do komunikatu. */
    public record Unsupported(String typeName) implements Geometry {}
}
