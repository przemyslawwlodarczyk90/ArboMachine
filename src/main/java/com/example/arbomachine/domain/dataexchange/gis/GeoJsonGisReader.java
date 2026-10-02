package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GeoJSON (FeatureCollection / Feature) jako warstwa GIS - dla importu obszarow grup drzew (etap 29). Uklad: brak
 * czlonu "crs" albo CRS84 / EPSG:4326 = WGS 84; czlon "crs" z EPSG:2176-2180 (stary GeoJSON z QGIS) - przeliczany.
 */
public final class GeoJsonGisReader {

    private static final Pattern EPSG_CODE = Pattern.compile("EPSG:*(\\d+)$");

    private GeoJsonGisReader() {
    }

    public static GisLayer read(byte[] content, ObjectMapper objectMapper) {
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (IOException invalidJson) {
            throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "geojson");
        }
        if (root == null || !root.isObject()) throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "geojson");
        List<JsonNode> featureNodes = new ArrayList<>();
        if ("FeatureCollection".equals(root.path("type").asText()) && root.path("features").isArray()) root.path("features").forEach(featureNodes::add);
        else if ("Feature".equals(root.path("type").asText())) featureNodes.add(root);
        else throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "geojson");

        List<GisFeature> features = new ArrayList<>();
        for (JsonNode featureNode : featureNodes) {
            Map<String, String> attributes = new LinkedHashMap<>();
            featureNode.path("properties").fields().forEachRemaining(property -> attributes.put(property.getKey(),
                    property.getValue().isNull() ? null : property.getValue().isValueNode() ? property.getValue().asText() : property.getValue().toString()));
            features.add(new GisFeature(features.size() + 1, geometryOf(featureNode.get("geometry")), attributes));
        }
        String crsName = root.path("crs").path("properties").path("name").asText("");
        if (crsName.isEmpty() || crsName.contains("CRS84")) return GisLayer.of("geojson", features, Optional.of(PolishGridCrs.WGS84));
        Matcher matcher = EPSG_CODE.matcher(crsName);
        Optional<PolishGridCrs> crs = matcher.find() ? PolishGridCrs.fromEpsgCode(Integer.parseInt(matcher.group(1))) : Optional.empty();
        return crs.isPresent() ? GisLayer.of("geojson", features, crs) : new GisLayer("geojson", features, Optional.empty(), crsName);
    }

    private static GisFeature.Geometry geometryOf(JsonNode geometry) {
        if (geometry == null || geometry.isNull()) return null;
        JsonNode coordinates = geometry.path("coordinates");
        return switch (geometry.path("type").asText()) {
            case "Point" -> coordinates.size() >= 2 ? new GisFeature.Point(coordinates.get(0).asDouble(), coordinates.get(1).asDouble())
                    : new GisFeature.Unsupported("Point");
            case "Polygon" -> polygonOf(coordinates);
            case "MultiPolygon" -> {
                List<GisFeature.Polygon> polygons = new ArrayList<>();
                coordinates.forEach(polygon -> polygons.add(polygonOf(polygon)));
                yield new GisFeature.MultiPolygon(polygons);
            }
            default -> new GisFeature.Unsupported(geometry.path("type").asText("?"));
        };
    }

    private static GisFeature.Polygon polygonOf(JsonNode rings) {
        List<List<double[]>> result = new ArrayList<>();
        rings.forEach(ring -> {
            List<double[]> positions = new ArrayList<>();
            ring.forEach(position -> positions.add(new double[]{position.path(0).asDouble(Double.NaN), position.path(1).asDouble(Double.NaN)}));
            result.add(positions);
        });
        return new GisFeature.Polygon(result);
    }
}
