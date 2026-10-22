package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer;
import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer.ExportCoordinates;
import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
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
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingDefinition;
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
 * GeoPackage drzew - "QGIS compatibility profile": warstwy "trees" (POINT), "stands" (POLYGON) i od etapu 34
 * "tree_markings" (POINT - jedno oznaczenie = jeden punkt w miejscu drzewa, zeby QGIS stylowal wiele oznaczen
 * jednego drzewa bez rozbijania tekstu). Atrybuty o krotkich, stabilnych nazwach GIS (TreeExchangeField.gisFieldName).
 * Etap 34: uklad wybierany przy eksporcie (WGS 84 domyslnie, PL-2000, PL-1992 - wszystkie warstwy w tym samym)
 * i style QML zapisane w tabeli layer_styles (QGIS wczytuje je sam przy dodaniu warstwy). Drzewa bez lokalizacji
 * sa pomijane. Biblioteka NGA GeoPackage pracuje na pliku, wiec eksport powstaje w pliku tymczasowym, zawsze usuwanym.
 */
@Component
public class TreeGeoPackageExporter {

    public static final String LAYER_NAME = "trees";
    public static final String STANDS_LAYER_NAME = "stands";
    public static final String MARKINGS_LAYER_NAME = "tree_markings";
    /** Tabela stylow QGIS w GeoPackage (nazwa i kolumny jak w QGIS - wtedy styl laduje sie jako domyslny). */
    public static final String LAYER_STYLES_TABLE = "layer_styles";
    public static final String GEOMETRY_COLUMN = "geom";
    public static final String CONTENT_TYPE = "application/geopackage+sqlite3";

    private final CoordinateTransformer defaultCoordinateTransformer;
    private final QgisLayerStyles qgisLayerStyles;
    private final TreeMarkingCatalog treeMarkingCatalog;

    public TreeGeoPackageExporter(CoordinateTransformer coordinateTransformer, QgisLayerStyles qgisLayerStyles,
                                  TreeMarkingCatalog treeMarkingCatalog) {
        this.defaultCoordinateTransformer = coordinateTransformer;
        this.qgisLayerStyles = qgisLayerStyles;
        this.treeMarkingCatalog = treeMarkingCatalog;
    }

    /** Eksport w WGS 84 (EPSG:4326). */
    public ExportedFile export(ProjectExportData data) {
        return export(data, defaultCoordinateTransformer);
    }

    /** Etap 34: eksport w wybranym ukladzie - wszystkie warstwy w tym samym. */
    public ExportedFile export(ProjectExportData data, CoordinateTransformer coordinateTransformer) {
        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("arbomachine-gpkg-");
            File geoPackageFile = temporaryDirectory.resolve("trees.gpkg").toFile();
            GeoPackageManager.create(geoPackageFile);
            try (GeoPackage geoPackage = GeoPackageManager.open(geoPackageFile)) {
                SpatialReferenceSystem srs = spatialReferenceSystemOf(geoPackage, coordinateTransformer);
                writeTreesLayer(geoPackage, data, coordinateTransformer, srs);
                writeStandsLayer(geoPackage, data, coordinateTransformer, srs);
                writeMarkingsLayer(geoPackage, data, coordinateTransformer, srs);
                writeLayerStyles(geoPackage);
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

    private static SpatialReferenceSystem spatialReferenceSystemOf(GeoPackage geoPackage, CoordinateTransformer coordinateTransformer)
            throws SQLException {
        String targetCrsCode = coordinateTransformer.targetCrsCode();
        long epsgCode = Long.parseLong(targetCrsCode.substring(targetCrsCode.indexOf(':') + 1));
        PolishGridCrs polishGridCrs = PolishGridCrs.fromEpsgCode((int) epsgCode).filter(crs -> crs != PolishGridCrs.WGS84).orElse(null);
        if (polishGridCrs == null) {
            return geoPackage.getSpatialReferenceSystemDao().getOrCreateCode(ProjectionConstants.AUTHORITY_EPSG, epsgCode);
        }
        // Etap 34: uklady polskie zapisujemy sami (srs_id = kod EPSG) - biblioteka nie zna ich definicji.
        SpatialReferenceSystem srs = new SpatialReferenceSystem();
        srs.setSrsName(polishGridCrs.displayName());
        srs.setSrsId(epsgCode);
        srs.setOrganization(ProjectionConstants.AUTHORITY_EPSG);
        srs.setOrganizationCoordsysId(epsgCode);
        srs.setDefinition(polishGridCrs.wktDefinition());
        srs.setDescription(polishGridCrs.displayName());
        geoPackage.getSpatialReferenceSystemDao().create(srs);
        return srs;
    }

    private void writeTreesLayer(GeoPackage geoPackage, ProjectExportData data, CoordinateTransformer coordinateTransformer,
                                 SpatialReferenceSystem srs) throws SQLException {
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
        geoPackage.createFeatureTable(FeatureTableMetadata.create(geometryColumns, attributeColumns,
                boundingBoxOf(locatedTrees, coordinateTransformer)));

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

    /** Warstwa "stands" (etap 29): grupy drzew jako wielokaty z nazwa, polem i numerami drzew (etap 34: w ukladzie eksportu). */
    private void writeStandsLayer(GeoPackage geoPackage, ProjectExportData data, CoordinateTransformer coordinateTransformer,
                                  SpatialReferenceSystem srs) throws SQLException {        GeometryColumns geometryColumns = new GeometryColumns();
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
        geoPackage.createFeatureTable(FeatureTableMetadata.create(geometryColumns, attributeColumns,
                boundingBoxOf(data.trees().stream().filter(TreeExportData::hasLocation).toList(), coordinateTransformer)));
        Contents contents = geoPackage.getContentsDao().queryForId(STANDS_LAYER_NAME);
        contents.setIdentifier(STANDS_LAYER_NAME);
        contents.setDescription("ArboMachine - grupy drzew projektu " + data.projectId() + " (" + coordinateTransformer.targetCrsCode() + ")");
        geoPackage.getContentsDao().update(contents);
        FeatureDao featureDao = geoPackage.getFeatureDao(STANDS_LAYER_NAME);
        for (TreeStandExportData stand : data.stands()) {
            mil.nga.sf.Polygon polygon = new mil.nga.sf.Polygon();
            for (List<double[]> ring : stand.rings()) {
                mil.nga.sf.LineString lineString = new mil.nga.sf.LineString();
                for (double[] position : ring) {
                    // Pierscienie grup sa w WGS 84 [dlugosc, szerokosc] - przeliczane do ukladu eksportu jak drzewa.
                    ExportCoordinates coordinates = coordinateTransformer.transform(BigDecimal.valueOf(position[1]), BigDecimal.valueOf(position[0]));
                    lineString.addPoint(new Point(coordinates.x().doubleValue(), coordinates.y().doubleValue()));
                }
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

    /**
     * Etap 34: warstwa "tree_markings" - jeden punkt na kazde oznaczenie drzewa (ta sama geometria co drzewo). Kod
     * oznaczenia (stabilny, do stylu i importu), nazwa i grupa po polsku, kolejnosc w katalogu (pozycja znaczka w stylu).
     */
    private void writeMarkingsLayer(GeoPackage geoPackage, ProjectExportData data, CoordinateTransformer coordinateTransformer,
                                    SpatialReferenceSystem srs) throws SQLException {
        GeometryColumns geometryColumns = new GeometryColumns();
        geometryColumns.setId(new TableColumnKey(MARKINGS_LAYER_NAME, GEOMETRY_COLUMN));
        geometryColumns.setGeometryType(GeometryType.POINT);
        geometryColumns.setZ((byte) 0);
        geometryColumns.setM((byte) 0);
        geometryColumns.setSrs(srs);
        List<FeatureColumn> attributeColumns = List.of(
                FeatureColumn.createColumn("tree_id", GeoPackageDataType.INTEGER),
                FeatureColumn.createColumn("inventory_no", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("marking_code", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("marking_label", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("category", GeoPackageDataType.TEXT),
                FeatureColumn.createColumn("catalog_order", GeoPackageDataType.INTEGER));
        List<TreeExportData> locatedTrees = data.trees().stream().filter(TreeExportData::hasLocation).toList();
        geoPackage.createFeatureTable(FeatureTableMetadata.create(geometryColumns, attributeColumns,
                boundingBoxOf(locatedTrees, coordinateTransformer)));
        Contents contents = geoPackage.getContentsDao().queryForId(MARKINGS_LAYER_NAME);
        contents.setIdentifier(MARKINGS_LAYER_NAME);
        contents.setDescription("ArboMachine - oznaczenia drzew projektu " + data.projectId() + " (" + coordinateTransformer.targetCrsCode() + ")");
        geoPackage.getContentsDao().update(contents);
        List<TreeMarkingDefinition> catalog = treeMarkingCatalog.allMarkings();
        FeatureDao featureDao = geoPackage.getFeatureDao(MARKINGS_LAYER_NAME);
        for (TreeExportData tree : locatedTrees) {
            ExportCoordinates coordinates = coordinateTransformer.transform(tree.latitude(), tree.longitude());
            for (String code : tree.markingCodes()) {
                TreeMarkingDefinition marking = catalog.stream().filter(definition -> definition.code().equals(code)).findFirst().orElse(null);
                if (marking == null) continue;
                FeatureRow row = featureDao.newRow();
                row.setGeometry(GeoPackageGeometryData.create(srs.getSrsId(),
                        new Point(coordinates.x().doubleValue(), coordinates.y().doubleValue())));
                row.setValue("tree_id", tree.treeId());
                row.setValue("inventory_no", tree.inventoryNumber());
                row.setValue("marking_code", code);
                row.setValue("marking_label", marking.displayName());
                row.setValue("category", marking.category().displayName());
                row.setValue("catalog_order", (long) catalog.indexOf(marking));
                featureDao.insert(row);
            }
        }
    }

    /**
     * Etap 34: style QML w tabeli layer_styles (ta sama struktura, ktora tworzy QGIS przy "Zapisz styl w bazie") -
     * QGIS przy dodaniu warstwy z tego pliku uzywa stylu oznaczonego jako domyslny. Tabela zarejestrowana w
     * gpkg_contents jako "attributes", zeby plik pozostal poprawnym GeoPackage.
     */
    private void writeLayerStyles(GeoPackage geoPackage) {
        geoPackage.execSQL("CREATE TABLE " + LAYER_STYLES_TABLE + " (id INTEGER PRIMARY KEY AUTOINCREMENT, f_table_catalog TEXT(256), "
                + "f_table_schema TEXT(256), f_table_name TEXT(256), f_geometry_column TEXT(256), styleName TEXT(30), styleQML TEXT, "
                + "styleSLD TEXT, useAsDefault BOOLEAN, description TEXT, owner TEXT(30), ui TEXT(30), update_time DATETIME DEFAULT CURRENT_TIMESTAMP)");
        geoPackage.execSQL("INSERT INTO gpkg_contents (table_name, data_type, identifier, description, last_change) VALUES ('"
                + LAYER_STYLES_TABLE + "', 'attributes', '" + LAYER_STYLES_TABLE + "', 'Style QGIS warstw ArboMachine', "
                + "strftime('%Y-%m-%dT%H:%M:%fZ','now'))");
        insertLayerStyle(geoPackage, LAYER_NAME, "ArboMachine - drzewa", qgisLayerStyles.treesStyle());
        insertLayerStyle(geoPackage, STANDS_LAYER_NAME, "ArboMachine - grupy drzew", qgisLayerStyles.standsStyle());
        insertLayerStyle(geoPackage, MARKINGS_LAYER_NAME, "ArboMachine - oznaczenia", qgisLayerStyles.treeMarkingsStyle());
    }

    private static void insertLayerStyle(GeoPackage geoPackage, String tableName, String styleName, String styleQml) {
        geoPackage.execSQL("INSERT INTO " + LAYER_STYLES_TABLE + " (f_table_catalog, f_table_schema, f_table_name, f_geometry_column, "
                + "styleName, styleQML, styleSLD, useAsDefault, description, owner, ui) VALUES ('', '', " + sqlText(tableName) + ", "
                + sqlText(GEOMETRY_COLUMN) + ", " + sqlText(styleName) + ", " + sqlText(styleQml) + ", '', 1, "
                + sqlText("Styl V1 z ArboMachine (etap 34)") + ", '', '')");
    }

    /** Literal tekstowy SQLite - tylko stale i style generowane w aplikacji, apostrofy podwojone. */
    private static String sqlText(String text) {
        return "'" + text.replace("'", "''") + "'";
    }

    private BoundingBox boundingBoxOf(List<TreeExportData> locatedTrees, CoordinateTransformer coordinateTransformer) {
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
