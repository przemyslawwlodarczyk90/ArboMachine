package com.example.arbomachine.domain.treestand.geometry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Obszar grupy drzew (etap 29) - wielokat w WGS 84: pierwszy pierscien = obrys, kolejne = dziury. Format zapisu
 * i API: geometria GeoJSON "Polygon" (pozycje [dlugosc, szerokosc]). V1 bez PostGIS: walidacja, pole powierzchni
 * i "punkt w obszarze" liczone tutaj. Bez sprawdzania samoprzeciec (DO_DOPRECYZOWANIA).
 */
public final class StandPolygon {

    public static final int MAXIMUM_VERTEX_COUNT = 10_000;
    /** Promien z formuly pola na kuli (jak w popularnych bibliotekach GIS) - wystarczy dla przegladu, nie do geodezji. */
    private static final double EARTH_RADIUS_METERS = 6_378_137.0;

    private final List<List<double[]>> rings;

    private StandPolygon(List<List<double[]>> rings) {
        this.rings = rings;
    }

    /** Pierscienie [dlugosc, szerokosc]; niezamkniete sa zamykane. Rzuca InvalidStandGeometryException. */
    public static StandPolygon ofRings(List<List<double[]>> rawRings) {
        if (rawRings == null || rawRings.isEmpty()) throw new InvalidStandGeometryException("Polygon has no rings");
        List<List<double[]>> rings = new ArrayList<>();
        int vertexCount = 0;
        for (List<double[]> rawRing : rawRings) {
            List<double[]> ring = new ArrayList<>();
            for (double[] position : rawRing) {
                if (position.length < 2 || !Double.isFinite(position[0]) || !Double.isFinite(position[1])
                        || Math.abs(position[0]) > 180 || Math.abs(position[1]) > 90) {
                    throw new InvalidStandGeometryException("Polygon coordinates must be WGS 84 longitude/latitude");
                }
                ring.add(new double[]{round7(position[0]), round7(position[1])});
            }
            if (!ring.isEmpty() && !samePosition(ring.getFirst(), ring.getLast())) ring.add(ring.getFirst().clone());
            if (ring.size() < 4 || ring.stream().map(p -> p[0] + ";" + p[1]).distinct().count() < 3) {
                throw new InvalidStandGeometryException("Polygon ring must have at least 3 distinct points");
            }
            vertexCount += ring.size();
            rings.add(ring);
        }
        if (vertexCount > MAXIMUM_VERTEX_COUNT) throw new InvalidStandGeometryException("Polygon has too many points");
        StandPolygon polygon = new StandPolygon(rings);
        if (polygon.areaSquareMeters() <= 0) throw new InvalidStandGeometryException("Polygon area must be greater than zero");
        return polygon;
    }

    /** Geometria GeoJSON: "Polygon" albo "MultiPolygon" z jednym wielokatem. */
    public static StandPolygon fromGeoJson(JsonNode geometry) {
        if (geometry == null || !geometry.isObject()) throw new InvalidStandGeometryException("Geometry is required");
        String type = geometry.path("type").asText();
        JsonNode coordinates = geometry.path("coordinates");
        if ("MultiPolygon".equals(type) && coordinates.isArray() && coordinates.size() == 1) {
            coordinates = coordinates.get(0);
        } else if (!"Polygon".equals(type)) {
            throw new InvalidStandGeometryException("Geometry must be a Polygon");
        }
        if (!coordinates.isArray()) throw new InvalidStandGeometryException("Polygon coordinates are missing");
        List<List<double[]>> rings = new ArrayList<>();
        for (JsonNode ringNode : coordinates) {
            if (!ringNode.isArray()) throw new InvalidStandGeometryException("Polygon ring must be an array");
            List<double[]> ring = new ArrayList<>();
            for (JsonNode positionNode : ringNode) {
                if (!positionNode.isArray() || positionNode.size() < 2 || !positionNode.get(0).isNumber() || !positionNode.get(1).isNumber()) {
                    throw new InvalidStandGeometryException("Polygon position must be [longitude, latitude]");
                }
                ring.add(new double[]{positionNode.get(0).asDouble(), positionNode.get(1).asDouble()});
            }
            rings.add(ring);
        }
        return ofRings(rings);
    }

    public static StandPolygon fromGeoJsonText(String geoJson, ObjectMapper objectMapper) {
        try {
            return fromGeoJson(objectMapper.readTree(geoJson));
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalidJson) {
            throw new InvalidStandGeometryException("Geometry is not valid JSON");
        }
    }

    public ObjectNode toGeoJson(ObjectMapper objectMapper) {
        ObjectNode geometry = objectMapper.createObjectNode();
        geometry.put("type", "Polygon");
        ArrayNode coordinates = geometry.putArray("coordinates");
        for (List<double[]> ring : rings) {
            ArrayNode ringNode = coordinates.addArray();
            for (double[] position : ring) ringNode.addArray().add(position[0]).add(position[1]);
        }
        return geometry;
    }

    public List<List<double[]>> rings() {
        return rings.stream().map(ring -> ring.stream().map(double[]::clone).toList()).toList();
    }

    public int vertexCount() {
        return rings.stream().mapToInt(List::size).sum();
    }

    /** Pole w m2: obrys minus dziury (pole na kuli; blad rzedu ulamka procenta - do przegladu, nie do operatu). */
    public double areaSquareMeters() {
        double area = Math.abs(ringArea(rings.getFirst()));
        for (List<double[]> hole : rings.subList(1, rings.size())) area -= Math.abs(ringArea(hole));
        return area;
    }

    public BigDecimal roundedAreaSquareMeters() {
        return BigDecimal.valueOf(areaSquareMeters()).setScale(2, RoundingMode.HALF_UP);
    }

    /** Czy punkt (WGS 84) lezy w obrysie i poza dziurami (promien - wystarczy dla obszarow rzedu hektarow). */
    public boolean contains(double longitude, double latitude) {
        if (!ringContains(rings.getFirst(), longitude, latitude)) return false;
        return rings.subList(1, rings.size()).stream().noneMatch(hole -> ringContains(hole, longitude, latitude));
    }

    /** Srodek obrysu (srednia wierzcholkow) - do etykiet i dopasowan, nie do pomiarow. */
    public double[] approximateCenter() {
        List<double[]> outer = rings.getFirst().subList(0, rings.getFirst().size() - 1);
        return new double[]{outer.stream().mapToDouble(p -> p[0]).average().orElse(0),
                outer.stream().mapToDouble(p -> p[1]).average().orElse(0)};
    }

    private static boolean ringContains(List<double[]> ring, double longitude, double latitude) {
        boolean inside = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            double[] a = ring.get(i);
            double[] b = ring.get(j);
            if ((a[1] > latitude) != (b[1] > latitude)
                    && longitude < (b[0] - a[0]) * (latitude - a[1]) / (b[1] - a[1]) + a[0]) {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Pole pierscienia na kuli (Chamberlain i Duquette, "Some algorithms for polygons on a sphere"). */
    private static double ringArea(List<double[]> ring) {
        int size = ring.size();
        if (size < 4) return 0;
        double total = 0;
        for (int i = 0; i < size - 1; i++) {
            double[] lower = ring.get(i);
            double[] middle = ring.get((i + 1) % (size - 1));
            double[] upper = ring.get((i + 2) % (size - 1));
            total += (Math.toRadians(upper[0]) - Math.toRadians(lower[0])) * Math.sin(Math.toRadians(middle[1]));
        }
        return total * EARTH_RADIUS_METERS * EARTH_RADIUS_METERS / 2;
    }

    private static boolean samePosition(double[] first, double[] second) {
        return first[0] == second[0] && first[1] == second[1];
    }

    private static double round7(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP).doubleValue();
    }
}
