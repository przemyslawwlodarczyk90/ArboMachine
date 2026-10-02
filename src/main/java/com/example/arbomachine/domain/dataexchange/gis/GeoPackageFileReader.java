package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureResultSet;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.sf.Geometry;
import mil.nga.sf.LineString;
import mil.nga.sf.MultiPolygon;
import mil.nga.sf.Point;
import mil.nga.sf.Polygon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GeoPackage (etap 29, ta sama biblioteka NGA co eksport): kazda tabela obiektow = warstwa. Uklad z kodu SRS
 * warstwy (4326, 2176-2180); inny kod -> warstwa z nieobslugiwanym ukladem. Plik czytany z kopii tymczasowej.
 */
public final class GeoPackageFileReader {

    private GeoPackageFileReader() {
    }

    public static List<GisLayer> read(byte[] content) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile("arbomachine-import-", ".gpkg");
            Files.write(temporaryFile, content);
            try (GeoPackage geoPackage = GeoPackageManager.open(temporaryFile.toFile(), false)) {
                List<GisLayer> layers = new ArrayList<>();
                for (String tableName : geoPackage.getFeatureTables()) layers.add(readLayer(geoPackage.getFeatureDao(tableName), tableName));
                return layers;
            }
        } catch (IOException | RuntimeException unreadable) {
            if (unreadable instanceof GisFileException gisFileException) throw gisFileException;
            throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "gpkg");
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // plik tymczasowy usunie system
                }
            }
        }
    }

    private static GisLayer readLayer(FeatureDao featureDao, String tableName) {
        long srsId = featureDao.getGeometryColumns().getSrsId();
        Optional<PolishGridCrs> crs = srsId == 4326 || srsId == 0 || srsId == -1 ? Optional.of(PolishGridCrs.WGS84)
                : PolishGridCrs.fromEpsgCode((int) srsId);
        String geometryColumn = featureDao.getGeometryColumnName();
        List<GisFeature> features = new ArrayList<>();
        FeatureResultSet resultSet = featureDao.queryForAll();
        try {
            int number = 0;
            while (resultSet.moveToNext()) {
                FeatureRow row = resultSet.getRow();
                Map<String, String> attributes = new LinkedHashMap<>();
                for (String columnName : row.getColumnNames()) {
                    if (columnName.equals(geometryColumn) || columnName.equals(featureDao.getPkColumnName())) continue;
                    Object value = row.getValue(columnName);
                    attributes.put(columnName, value == null ? null : value.toString());
                }
                GeoPackageGeometryData geometryData = row.getGeometry();
                features.add(new GisFeature(++number, geometryOf(geometryData == null ? null : geometryData.getGeometry()), attributes));
            }
        } finally {
            resultSet.close();
        }
        return crs.isPresent() ? GisLayer.of(tableName, features, crs) : new GisLayer(tableName, features, Optional.empty(), "EPSG:" + srsId);
    }

    private static GisFeature.Geometry geometryOf(Geometry geometry) {
        if (geometry == null) return null;
        if (geometry instanceof Point point) return new GisFeature.Point(point.getX(), point.getY());
        if (geometry instanceof Polygon polygon) return polygonOf(polygon);
        if (geometry instanceof MultiPolygon multiPolygon) {
            return new GisFeature.MultiPolygon(multiPolygon.getPolygons().stream().map(GeoPackageFileReader::polygonOf).toList());
        }
        return new GisFeature.Unsupported(geometry.getGeometryType().getName());
    }

    private static GisFeature.Polygon polygonOf(Polygon polygon) {
        List<List<double[]>> rings = new ArrayList<>();
        for (LineString ring : polygon.getRings()) {
            rings.add(ring.getPoints().stream().map(point -> new double[]{point.getX(), point.getY()}).toList());
        }
        return new GisFeature.Polygon(rings);
    }
}
