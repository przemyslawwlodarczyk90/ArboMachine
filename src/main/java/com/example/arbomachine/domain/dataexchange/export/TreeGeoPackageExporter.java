package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer;
import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer.ExportCoordinates;
import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.contents.Contents;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.db.TableColumnKey;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.features.user.FeatureTableMetadata;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.srs.SpatialReferenceSystem;
import mil.nga.proj.ProjectionConstants;
import mil.nga.sf.GeometryType;
import mil.nga.sf.Point;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * GeoPackage drzew - "QGIS compatibility profile V1": plik .gpkg z jedna warstwa "trees", geometria POINT,
 * EPSG:4326 (WGS 84), atrybuty o krotkich, stabilnych nazwach GIS (TreeExchangeField.gisFieldName). Drzewa bez
 * lokalizacji sa pomijane, jak w GeoJSON. Biblioteka NGA GeoPackage (SQLite w jarze) pracuje na pliku, wiec
 * eksport powstaje w pliku tymczasowym, ktory jest zawsze usuwany. Bez stylu QGIS (QML) i bez projektu QGZ.
 */
@Component
public class TreeGeoPackageExporter {

    public static final String LAYER_NAME = "trees";
    public static final String STANDS_LAYER_NAME = "stands";
    public static final String GEOMETRY_COLUMN = "geom";
    public static final String CONTENT_TYPE = "application/geopackage+sqlite3";

    private final CoordinateTransformer coordinateTransformer;

    public TreeGeoPackageExporter(CoordinateTransformer coordinateTransformer) {
        this.coordinateTransformer = coordinateTransformer;
    }

    public ExportedFile export(ProjectExportData data) {
        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("arbomachine-gpkg-");
            File geoPackageFile = temporaryDirectory.resolve("trees.gpkg").toFile();
            GeoPackageManager.create(geoPackageFile);
            try (GeoPackage geoPackage = GeoPackageManager.open(geoPackageFile)) {
                writeTreesLayer(geoPackage, data);
                writeStandsLayer(geoPackage, data);
            }
            return new ExportedFile(Files.readAllBytes(geoPackageFile.toPath()), CONTENT_TYPE, ExportedFile.fileNameOf(data, "gpkg"));
        } catch (IOException writingError) {
            throw new UncheckedIOException(writingError);
        } catch (SQLException sqliteError) {
            throw new IllegalStateException("Could not write GeoPackage", sqliteError);
        } finally {
            deleteQuietly(temporaryDirectory);
        }
    }

    private void writeTreesLayer(GeoPackage geoPackage, ProjectExportData data) throws SQLException {
        String epsgCode = coordinateTransformer.targetCrsCode().substring(coordinateTransformer.targetCrsCode().indexOf(':') + 1);
        SpatialReferenceSystem srs = geoPackage.getSpatialReferenceSystemDao()
                .getOrCreateCode(ProjectionConstants.AUTHORITY_EPSG, Long.parseLong(epsgCode));

        GeometryColumns geometryColumns = new GeometryColumns();
        geometryColumns.setId(new TableColumnKey(LAYER_NAME, GEOMETRY_COLUMN));
        geometryColumns.setGeometryType(GeometryType.POINT);
        geometryColumns.setZ((byte) 0);
        geometryColumns.setM((byte) 0);
        geometryColumns.setSrs(srs);

        List<FeatureColumn> attributeColumns = new ArrayList<>();
        for (TreeExchangeField field : TreeExchangeField.GIS_ATTRIBUTES) {
            attributeColumns.add(FeatureColumn.createColumn(field.gisFieldName(), dataTypeOf(field)));
        }
        List<TreeExportData> locatedTrees = data.trees().stream().filter(TreeExportData::hasLocation).toList();
        geoPackage.createFeatureTable(FeatureTableMetadata.create(geometryColumns, attributeColumns, boundingBoxOf(locatedTrees)));

        Contents contents = geoPackage.getContentsDao().queryForId(LAYER_NAME);
        contents.setIdentifier(LAYER_NAME);
        contents.setDescription("ArboMachine - drzewa projektu " + data.projectId() + " (schemat " + TreeExchangeField.SCHEMA_VERSION + ", "
                + coordinateTransformer.targetCrsCode() + ")");
        geoPackage.getContentsDao().update(contents);

        FeatureDao featureDao = geoPackage.getFeatureDao(LAYER_NAME);
        for (TreeExportData tree : locatedTrees) {
            FeatureRow row = featureDao.newRow();
            ExportCoordinates coordinates = coordinateTransformer.transform(tree.latitude(), tree.longitude());
            row.setGeometry(GeoPackageGeometryData.create(srs.getSrsId(),
                    new Point(coordinates.x().doubleValue(), coordinates.y().doubleValue())));
            for (TreeExchangeField field : TreeExchangeField.GIS_ATTRIBUTES) {
                Object value = tree.valueOf(field);
                row.setValue(field.gisFieldName(), value instanceof BigDecimal decimal ? decimal.doubleValue() : value);
            }
            featureDao.insert(row);
        }
    }

    /** Warstwa "stands" (etap 29): grupy drzew jako wielokaty WGS 84 z nazwa, polem i numerami drzew. */
    private void writeStandsLayer(GeoPackage geoPackage, ProjectExportData data) throws SQLException {
        SpatialReferenceSystem srs = geoPackage.getSpatialReferenceSystemDao()
                .getOrCreateCode(ProjectionConstants.AUTHORITY_EPSG, 4326);
        GeometryColumns geometryColumns = new GeometryColumns();
        geometryColumns.setId(new TableColumnKey(STANDS_LAYER_NAME, GEOMETRY_COLUMN));
        geometryColumns.setGeometryType(GeometryType.POLYGON);
        geometryColumns.setZ((byte) 0);
        geometryColumns.setM((byte) 0);
        geometryColumns.setSrs(srs);
        List<FeatureColumn> attributeColumns = List.of(
                FeatureColumn.createColumn("stand_id", GeoPackageDataType.INTEGER),
                FeatureColumn.createColumn("name", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("notes", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("area_m2", GeoPackageDataType.REAL),
                FeatureColumn.createColumn("tree_count", GeoPackageDataType.INTEGER),
                FeatureColumn.createColumn("tree_nos", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("source", GeoPackageDataType.TEXT));
        geoPackage.createFeatureTable(FeatureTableMetadata.create(geometryColumns, attributeColumns, new BoundingBox(-180, -90, 180, 90)));
        Contents contents = geoPackage.getContentsDao().queryForId(STANDS_LAYER_NAME);
        contents.setIdentifier(STANDS_LAYER_NAME);
        contents.setDescription("ArboMachine - grupy drzew projektu " + data.projectId() + " (EPSG:4326)");
        geoPackage.getContentsDao().update(contents);
        FeatureDao featureDao = geoPackage.getFeatureDao(STANDS_LAYER_NAME);
        for (TreeStandExportData stand : data.stands()) {
            mil.nga.sf.Polygon polygon = new mil.nga.sf.Polygon();
            for (List<double[]> ring : stand.rings()) {
                mil.nga.sf.LineString lineString = new mil.nga.sf.LineString();
                for (double[] position : ring) lineString.addPoint(new Point(position[0], position[1]));
                polygon.addRing(lineString);
            }
            FeatureRow row = featureDao.newRow();
            row.setGeometry(GeoPackageGeometryData.create(srs.getSrsId(), polygon));
            row.setValue("stand_id", stand.standId());
            row.setValue("name", stand.name());
            row.setValue("notes", stand.notes());
            row.setValue("area_m2", stand.areaSquareMeters() == null ? null : stand.areaSquareMeters().doubleValue());
            row.setValue("tree_count", (long) stand.treeInventoryNumbers().size());
            row.setValue("tree_nos", stand.treeNumbersText());
            row.setValue("source", stand.source());
            featureDao.insert(row);
        }
    }

    private BoundingBox boundingBoxOf(List<TreeExportData> locatedTrees) {
        if (locatedTrees.isEmpty()) return new BoundingBox(-180, -90, 180, 90);
        double minimumX = Double.MAX_VALUE, minimumY = Double.MAX_VALUE, maximumX = -Double.MAX_VALUE, maximumY = -Double.MAX_VALUE;
        for (TreeExportData tree : locatedTrees) {
            ExportCoordinates coordinates = coordinateTransformer.transform(tree.latitude(), tree.longitude());
            minimumX = Math.min(minimumX, coordinates.x().doubleValue());
            maximumX = Math.max(maximumX, coordinates.x().doubleValue());
            minimumY = Math.min(minimumY, coordinates.y().doubleValue());
            maximumY = Math.max(maximumY, coordinates.y().doubleValue());
        }
        return new BoundingBox(minimumX, minimumY, maximumX, maximumY);
    }

    private static GeoPackageDataType dataTypeOf(TreeExchangeField field) {
        return switch (field.valueType()) {
            case INTEGER -> GeoPackageDataType.INTEGER;
            case DECIMAL -> GeoPackageDataType.REAL;
            case TEXT -> GeoPackageDataType.TEXT;
        };
    }

    private static void deleteQuietly(Path directory) {
        if (directory == null) return;
        try (var files = Files.walk(directory)) {
            files.sorted((first, second) -> second.compareTo(first)).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // plik tymczasowy - w najgorszym razie zostanie w katalogu tymczasowym systemu
        }
    }
}
