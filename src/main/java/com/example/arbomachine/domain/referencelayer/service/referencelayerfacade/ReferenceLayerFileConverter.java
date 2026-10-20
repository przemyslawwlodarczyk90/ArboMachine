package com.example.arbomachine.domain.referencelayer.service.referencelayerfacade;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader;
import com.example.arbomachine.domain.dataexchange.gis.DxfLayerExtractor;
import com.example.arbomachine.domain.dataexchange.gis.GeoJsonGisReader;
import com.example.arbomachine.domain.dataexchange.gis.GeoPackageFileReader;
import com.example.arbomachine.domain.dataexchange.gis.GisFeature;
import com.example.arbomachine.domain.dataexchange.gis.GisFileException;
import com.example.arbomachine.domain.dataexchange.gis.GisLayer;
import com.example.arbomachine.domain.dataexchange.gis.ShapefileZipReader;
import com.example.arbomachine.domain.dataexchange.imports.TreeImportFormat;
import com.example.arbomachine.domain.referencelayer.exception.ReferenceLayerFileRejectedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Plik GIS/CAD -> GeoJSON FeatureCollection w WGS 84 (etap 31). Korzysta z czytnikow modulu dataexchange (waski kontrakt:
 * GisLayer / GisFeature). Uklad: opisany w pliku albo odgadniety z pierwszego wierzcholka (PL-2000 / PL-1992 / WGS 84).
 * Atrybuty: do MAXIMUM_ATTRIBUTES kolumn jako tekst (skracany) + "warstwa" (nazwa warstwy pliku). Obiekty bez geometrii
 * i o nieobslugiwanej geometrii sa pomijane. Limity chronia przegladarke tabletu przed ogromnymi plikami.
 */
@Component
public class ReferenceLayerFileConverter {

    public static final int MAXIMUM_FILE_SIZE_BYTES = 15 * 1024 * 1024;
    public static final int MAXIMUM_FEATURES = 20_000;
    public static final int MAXIMUM_VERTICES = 400_000;
    private static final int MAXIMUM_ATTRIBUTES = 20;
    private static final int MAXIMUM_ATTRIBUTE_LENGTH = 200;
    private static final int COORDINATE_SCALE = 7;

    private final ObjectMapper objectMapper;

    public ReferenceLayerFileConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ConvertedReferenceLayer(String format, String sourceCrs, int featureCount, String geoJson) {}

    public ConvertedReferenceLayer convert(String fileName, byte[] content) {
        TreeImportFormat format = TreeImportFormat.fromFileName(fileName)
                .filter(candidate -> candidate != TreeImportFormat.CSV && candidate != TreeImportFormat.XLSX)
                .orElseThrow(() -> new ReferenceLayerFileRejectedException("UNSUPPORTED_FILE_FORMAT", null));
        if (content == null || content.length == 0) throw new ReferenceLayerFileRejectedException("EMPTY_FILE", null);
        if (content.length > MAXIMUM_FILE_SIZE_BYTES) throw new ReferenceLayerFileRejectedException("FILE_TOO_LARGE", null);
        List<GisLayer> layers;
        try {
            layers = layersOf(format, content);
        } catch (GisFileException gisFileException) {
            throw new ReferenceLayerFileRejectedException(gisFileException.reason() == GisFileException.Reason.UNSUPPORTED_CRS
                    ? "UNSUPPORTED_CRS" : "UNREADABLE_FILE", gisFileException.detail());
        }

        ObjectNode collection = objectMapper.createObjectNode().put("type", "FeatureCollection");
        ArrayNode features = collection.putArray("features");
        Set<String> sourceCrsCodes = new LinkedHashSet<>();
        int[] vertexCount = {0};
        for (GisLayer layer : layers) {
            PolishGridCrs crs = crsOf(layer);
            boolean hasGeometry = false;
            for (GisFeature feature : layer.features()) {
                Optional<ObjectNode> geometry = geometryJson(feature.geometry(), crs, vertexCount);
                if (geometry.isEmpty()) continue;
                hasGeometry = true;
                if (features.size() >= MAXIMUM_FEATURES) throw new ReferenceLayerFileRejectedException("TOO_MANY_FEATURES", String.valueOf(MAXIMUM_FEATURES));
                if (vertexCount[0] > MAXIMUM_VERTICES) throw new ReferenceLayerFileRejectedException("TOO_MANY_FEATURES", String.valueOf(MAXIMUM_VERTICES));
                ObjectNode featureJson = features.addObject().put("type", "Feature");
                featureJson.set("geometry", geometry.get());
                featureJson.set("properties", propertiesOf(layer.name(), feature.attributes()));
            }
            if (hasGeometry) sourceCrsCodes.add(crs.code());
        }
        if (features.isEmpty()) throw new ReferenceLayerFileRejectedException("NO_FEATURES", null);
        return new ConvertedReferenceLayer(format.name(), String.join(", ", sourceCrsCodes), features.size(), collection.toString());
    }

    private List<GisLayer> layersOf(TreeImportFormat format, byte[] content) {
        return switch (format) {
            case SHAPEFILE -> ShapefileZipReader.read(content);
            case GEOPACKAGE -> GeoPackageFileReader.read(content);
            case DXF -> DxfLayerExtractor.referenceLayers(DxfDrawingReader.read(content));
            case GEOJSON -> List.of(GeoJsonGisReader.read(content, objectMapper));
            case CSV, XLSX -> List.of();
        };
    }

    private static PolishGridCrs crsOf(GisLayer layer) {
        if (layer.unsupportedCrsName() != null) throw new ReferenceLayerFileRejectedException("UNSUPPORTED_CRS", layer.unsupportedCrsName());
        if (layer.crs().isPresent()) return layer.crs().get();
        Optional<double[]> firstPosition = layer.features().stream().map(GisFeature::geometry).map(GisFeature::firstPositionOf)
                .flatMap(Optional::stream).findFirst();
        if (firstPosition.isEmpty()) return PolishGridCrs.WGS84;
        return PolishGridCrs.guessFromCoordinates(firstPosition.get()[0], firstPosition.get()[1])
                .orElseThrow(() -> new ReferenceLayerFileRejectedException("UNSUPPORTED_CRS", "?"));
    }

    private Optional<ObjectNode> geometryJson(GisFeature.Geometry geometry, PolishGridCrs crs, int[] vertexCount) {
        ObjectNode json = objectMapper.createObjectNode();
        switch (geometry) {
            case GisFeature.Point point -> json.put("type", "Point").set("coordinates", position(point.x(), point.y(), crs, vertexCount));
            case GisFeature.LineString line -> {
                if (line.points().size() < 2) return Optional.empty();
                json.put("type", "LineString").set("coordinates", positions(line.points(), crs, vertexCount));
            }
            case GisFeature.MultiLineString lines -> {
                ArrayNode coordinates = json.put("type", "MultiLineString").putArray("coordinates");
                lines.lines().stream().filter(line -> line.points().size() >= 2)
                        .forEach(line -> coordinates.add(positions(line.points(), crs, vertexCount)));
                if (coordinates.isEmpty()) return Optional.empty();
            }
            case GisFeature.Polygon polygon -> {
                if (polygon.rings().isEmpty() || polygon.rings().getFirst().size() < 3) return Optional.empty();
                json.put("type", "Polygon").set("coordinates", rings(polygon, crs, vertexCount));
            }
            case GisFeature.MultiPolygon polygons -> {
                ArrayNode coordinates = json.put("type", "MultiPolygon").putArray("coordinates");
                polygons.polygons().stream().filter(polygon -> !polygon.rings().isEmpty() && polygon.rings().getFirst().size() >= 3)
                        .forEach(polygon -> coordinates.add(rings(polygon, crs, vertexCount)));
                if (coordinates.isEmpty()) return Optional.empty();
            }
            case GisFeature.Unsupported ignored -> {
                return Optional.empty();
            }
            case null -> {
                return Optional.empty();
            }
        }
        return Optional.of(json);
    }

    private ArrayNode rings(GisFeature.Polygon polygon, PolishGridCrs crs, int[] vertexCount) {
        ArrayNode rings = objectMapper.createArrayNode();
        polygon.rings().forEach(ring -> rings.add(positions(ring, crs, vertexCount)));
        return rings;
    }

    private ArrayNode positions(List<double[]> points, PolishGridCrs crs, int[] vertexCount) {
        ArrayNode positions = objectMapper.createArrayNode();
        points.forEach(point -> positions.add(position(point[0], point[1], crs, vertexCount)));
        return positions;
    }

    private ArrayNode position(double x, double y, PolishGridCrs crs, int[] vertexCount) {
        vertexCount[0]++;
        double[] lonLat = crs.toWgs84(x, y);
        // Np. GeoJSON bez opisu ukladu z metrami PL-2000 albo uszkodzona liczba - lepiej odrzucic niz narysowac w oceanie.
        if (!Double.isFinite(lonLat[0]) || !Double.isFinite(lonLat[1]) || Math.abs(lonLat[0]) > 180 || Math.abs(lonLat[1]) > 90) {
            throw new ReferenceLayerFileRejectedException("INVALID_COORDINATES", x + ", " + y);
        }
        return objectMapper.createArrayNode().add(rounded(lonLat[0])).add(rounded(lonLat[1]));
    }

    private static BigDecimal rounded(double degrees) {
        return BigDecimal.valueOf(degrees).setScale(COORDINATE_SCALE, RoundingMode.HALF_UP);
    }

    private ObjectNode propertiesOf(String layerName, Map<String, String> attributes) {
        ObjectNode properties = objectMapper.createObjectNode().put("warstwa", layerName);
        List<Map.Entry<String, String>> entries = new ArrayList<>(attributes.entrySet());
        entries.stream().filter(entry -> entry.getValue() != null && !entry.getValue().isBlank()).limit(MAXIMUM_ATTRIBUTES)
                .forEach(entry -> properties.put(entry.getKey(), entry.getValue().length() > MAXIMUM_ATTRIBUTE_LENGTH
                        ? entry.getValue().substring(0, MAXIMUM_ATTRIBUTE_LENGTH) : entry.getValue()));
        return properties;
    }
}
