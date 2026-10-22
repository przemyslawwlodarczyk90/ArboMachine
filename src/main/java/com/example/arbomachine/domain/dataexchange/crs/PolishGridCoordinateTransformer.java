package com.example.arbomachine.domain.dataexchange.crs;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Etap 34: eksport w polskim ukladzie (PL-2000 strefa 5-8 albo PL-1992) - WGS84 z TreeLocation przeliczane tym samym
 * odwzorowaniem co import i karta drzewa (PolishGridCrs, zgodne z PROJ do 1 mm). Wynik: x = easting, y = northing
 * w metrach (kolejnosc GIS, jak zapisuja QGIS i GeoPackage), zaokraglone do milimetra.
 */
public class PolishGridCoordinateTransformer implements CoordinateTransformer {

    private static final int MILLIMETER_DECIMAL_PLACES = 3;

    private final PolishGridCrs polishGridCrs;

    public PolishGridCoordinateTransformer(PolishGridCrs polishGridCrs) {
        if (polishGridCrs == PolishGridCrs.WGS84) throw new IllegalArgumentException("Use Wgs84CoordinateTransformer for EPSG:4326");
        this.polishGridCrs = polishGridCrs;
    }

    @Override
    public String targetCrsCode() {
        return polishGridCrs.code();
    }

    @Override
    public ExportCoordinates transform(BigDecimal latitude, BigDecimal longitude) {
        double[] eastingNorthing = polishGridCrs.fromWgs84(longitude.doubleValue(), latitude.doubleValue());
        return new ExportCoordinates(millimeters(eastingNorthing[0]), millimeters(eastingNorthing[1]));
    }

    private static BigDecimal millimeters(double meters) {
        return BigDecimal.valueOf(meters).setScale(MILLIMETER_DECIMAL_PLACES, RoundingMode.HALF_UP);
    }
}
