package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
import com.example.arbomachine.domain.dataexchange.gis.GisFeature;
import com.example.arbomachine.domain.dataexchange.gis.GisFileException;
import com.example.arbomachine.domain.dataexchange.gis.GisLayer;
import com.example.arbomachine.domain.dataexchange.schema.FieldInventoryColumnAliases;
import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Warstwy punktowe pliku GIS/CAD (SHP, GeoPackage, DXF) -> wspolny model wierszy importu drzew (etap 29).
 * Atrybuty dopasowywane jak naglowki CSV (TreeExchangeField: polska etykieta, nazwa techniczna, krotka nazwa GIS,
 * oraz typowe naglowki aplikacji terenowych). Wspolrzedne przeliczane do WGS 84 (PL-2000 / PL-1992 -> EPSG:4326),
 * 7 miejsc po przecinku. Obiekt niepunktowy = blad wiersza; warstwa bez punktow = pominieta.
 */
final class GisTreeRows {

    private GisTreeRows() {
    }

    /** Uklad, z ktorego przeliczono wspolrzedne (do ostrzezenia COORDINATES_CONVERTED); null = WGS 84. */
    record ConvertedTreeFile(ParsedTreeImportFile file, PolishGridCrs sourceCrs) {}

    static ConvertedTreeFile fromLayers(List<GisLayer> layers) {
        List<GisLayer> pointLayers = layers.stream().filter(GisLayer::hasPoints).toList();
        if (pointLayers.isEmpty()) {
            boolean polygonsOnly = layers.stream().anyMatch(GisLayer::hasPolygons);
            throw new TreeImportFileException(polygonsOnly ? TreeImportIssueCode.NO_POINT_FEATURES : TreeImportIssueCode.NO_DATA_ROWS);
        }
        Set<TreeExchangeField> recognizedFields = new LinkedHashSet<>();
        Set<String> ignoredColumns = new LinkedHashSet<>();
        List<ImportedTreeRow> rows = new ArrayList<>();
        PolishGridCrs usedCrs = PolishGridCrs.WGS84;
        int rowNumber = 0;
        for (GisLayer layer : pointLayers) {
            PolishGridCrs crs = crsOf(layer);
            if (crs != PolishGridCrs.WGS84) usedCrs = crs;
            for (GisFeature feature : layer.features()) {
                rows.add(rowOf(++rowNumber, feature, crs, recognizedFields, ignoredColumns));
            }
        }
        if (!recognizedFields.contains(TreeExchangeField.INVENTORY_NUMBER)) {
            throw new TreeImportFileException(TreeImportIssueCode.MISSING_REQUIRED_COLUMN, TreeExchangeField.INVENTORY_NUMBER.gisFieldName());
        }
        return new ConvertedTreeFile(new ParsedTreeImportFile(recognizedFields, List.copyOf(ignoredColumns), rows),
                usedCrs == PolishGridCrs.WGS84 ? null : usedCrs);
    }

    /** Uklad warstwy: opisany w pliku, nieobslugiwany (blad) albo odgadniety z pierwszego obiektu z geometria. */
    static PolishGridCrs crsOf(GisLayer layer) {
        if (layer.unsupportedCrsName() != null) throw new TreeImportFileException(TreeImportIssueCode.UNSUPPORTED_CRS, layer.unsupportedCrsName());
        if (layer.crs().isPresent()) return layer.crs().get();
        Optional<double[]> firstPosition = layer.features().stream().map(GisFeature::geometry).map(GisTreeRows::firstPositionOf)
                .flatMap(Optional::stream).findFirst();
        if (firstPosition.isEmpty()) return PolishGridCrs.WGS84;
        return PolishGridCrs.guessFromCoordinates(firstPosition.get()[0], firstPosition.get()[1])
                .orElseThrow(() -> new TreeImportFileException(TreeImportIssueCode.UNSUPPORTED_CRS, "?"));
    }

    static Optional<double[]> firstPositionOf(GisFeature.Geometry geometry) {
        return GisFeature.firstPositionOf(geometry);
    }

    private static ImportedTreeRow rowOf(int rowNumber, GisFeature feature, PolishGridCrs crs,
                                         Set<TreeExchangeField> recognizedFields, Set<String> ignoredColumns) {
        Map<TreeExchangeField, ImportedCell> cells = new EnumMap<>(TreeExchangeField.class);
        List<TreeImportIssue> readingIssues = new ArrayList<>();
        feature.attributes().forEach((name, value) -> {
            Optional<TreeExchangeField> field = TreeExchangeField.fromHeader(name).or(() -> FieldInventoryColumnAliases.fieldOf(name));
            if (field.isEmpty() || !field.get().importable()
                    || field.get() == TreeExchangeField.LATITUDE || field.get() == TreeExchangeField.LONGITUDE) {
                ignoredColumns.add(name);
                return;
            }
            recognizedFields.add(field.get());
            cells.put(field.get(), new ImportedCell(field.get(), name, value));
        });
        if (feature.geometry() instanceof GisFeature.Point point) {
            double[] lonLat = crs.toWgs84(point.x(), point.y());
            cells.put(TreeExchangeField.LONGITUDE, new ImportedCell(TreeExchangeField.LONGITUDE, "geometria", decimal7(lonLat[0])));
            cells.put(TreeExchangeField.LATITUDE, new ImportedCell(TreeExchangeField.LATITUDE, "geometria", decimal7(lonLat[1])));
        } else if (feature.geometry() != null) {
            String typeName = switch (feature.geometry()) {
                case GisFeature.Polygon ignored -> "Polygon";
                case GisFeature.MultiPolygon ignored -> "MultiPolygon";
                case GisFeature.Unsupported unsupported -> unsupported.typeName();
                case GisFeature.Point ignored -> "Point";
                case GisFeature.LineString ignored -> "LineString";
                case GisFeature.MultiLineString ignored -> "MultiLineString";
            };
            readingIssues.add(new TreeImportIssue(rowNumber, "geometry", "geometria", typeName, TreeImportIssueCode.UNSUPPORTED_GEOMETRY));
        }
        // Brak atrybutu numeru (np. punkt bez opisu w DXF) - komorka pusta, walidacja zglosi brak numeru w tym wierszu.
        cells.putIfAbsent(TreeExchangeField.INVENTORY_NUMBER,
                new ImportedCell(TreeExchangeField.INVENTORY_NUMBER, TreeExchangeField.INVENTORY_NUMBER.polishLabel(), null));
        return new ImportedTreeRow(rowNumber, cells, readingIssues);
    }

    private static String decimal7(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP).toPlainString();
    }

    static TreeImportFileException fileExceptionOf(GisFileException gisFileException) {
        return switch (gisFileException.reason()) {
            case UNREADABLE_FILE -> new TreeImportFileException(TreeImportIssueCode.UNREADABLE_FILE);
            case UNSUPPORTED_CRS -> new TreeImportFileException(TreeImportIssueCode.UNSUPPORTED_CRS, gisFileException.detail());
            case NO_SHAPEFILE_IN_ZIP -> new TreeImportFileException(TreeImportIssueCode.NO_SHAPEFILE_IN_ZIP);
        };
    }
}
