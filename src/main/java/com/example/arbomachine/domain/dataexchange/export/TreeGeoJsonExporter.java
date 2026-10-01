package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer;
import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer.ExportCoordinates;
import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * GeoJSON drzew - "ArboMachine GeoJSON Schema V1" (RFC 7946): FeatureCollection, jedno drzewo z lokalizacja = jeden
 * Feature z geometria Point [dlugosc, szerokosc] w WGS 84 (EPSG:4326, bez czlonu "crs" - RFC 7946 zaklada WGS 84).
 * Drzewa BEZ lokalizacji sa pomijane (ich liczba jest w czlonie "arboMachine" i w UI przed eksportem).
 * Properties: stabilne nazwy techniczne (TreeExchangeField.technicalName), braki jako null, prace jako tablica.
 * Czlon "arboMachine" (dozwolony "foreign member") niesie wersje schematu - QGIS go ignoruje. Bez zdjec.
 */
@Component
public class TreeGeoJsonExporter {

    public static final String CONTENT_TYPE = "application/geo+json";

    private final ObjectMapper objectMapper;
    private final CoordinateTransformer coordinateTransformer;

    public TreeGeoJsonExporter(ObjectMapper objectMapper, CoordinateTransformer coordinateTransformer) {
        this.objectMapper = objectMapper;
        this.coordinateTransformer = coordinateTransformer;
    }

    public ExportedFile export(ProjectExportData data) {
        ObjectNode featureCollection = objectMapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");
        featureCollection.put("name", "trees");
        ObjectNode metadata = featureCollection.putObject("arboMachine");
        metadata.put("schemaVersion", TreeExchangeField.SCHEMA_VERSION);
        metadata.put("crs", coordinateTransformer.targetCrsCode());
        metadata.put("projectId", data.projectId());
        metadata.put("exportedAt", data.exportedAt().toString());
        metadata.put("treeCount", data.trees().size());
        metadata.put("treesWithoutLocationCount", data.treesWithoutLocationCount());

        ArrayNode features = featureCollection.putArray("features");
        for (TreeExportData tree : data.trees()) {
            if (!tree.hasLocation()) continue;
            ObjectNode feature = features.addObject();
            feature.put("type", "Feature");
            feature.put("id", tree.treeId());
            ObjectNode geometry = feature.putObject("geometry");
            geometry.put("type", "Point");
            ExportCoordinates coordinates = coordinateTransformer.transform(tree.latitude(), tree.longitude());
            geometry.putArray("coordinates").add(plain(coordinates.x())).add(plain(coordinates.y()));
            ObjectNode properties = feature.putObject("properties");
            for (TreeExchangeField field : TreeExchangeField.GIS_ATTRIBUTES) {
                putValue(properties, field.technicalName(), tree.valueOf(field));
            }
            ArrayNode works = properties.putArray("works");
            for (TreeWorkExportData work : tree.works()) {
                ObjectNode workNode = works.addObject();
                workNode.put("name", work.name());
                workNode.put("description", work.description());
                workNode.put("mapSymbolCode", work.mapSymbolCode());
            }
        }
        try {
            return new ExportedFile(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(featureCollection)
                    .getBytes(StandardCharsets.UTF_8), CONTENT_TYPE, ExportedFile.fileNameOf(data, "geojson"));
        } catch (JsonProcessingException serializationError) {
            throw new IllegalStateException("Could not serialize GeoJSON", serializationError);
        }
    }

    private static void putValue(ObjectNode properties, String name, Object value) {
        switch (value) {
            case null -> properties.putNull(name);
            case BigDecimal decimal -> properties.put(name, plain(decimal));
            case Integer integer -> properties.put(name, integer);
            case Long longValue -> properties.put(name, longValue);
            default -> properties.put(name, value.toString());
        }
    }

    /** 18.50 -> 18.5, 52.2297000 -> 52.2297 (bez notacji wykladniczej). */
    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }
}
