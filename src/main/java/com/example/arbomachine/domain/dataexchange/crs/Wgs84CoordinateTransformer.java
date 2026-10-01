package com.example.arbomachine.domain.dataexchange.crs;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** EPSG:4326 / WGS 84 - uklad TreeLocation, wiec "transformacja" tylko ustawia kolejnosc x = dlugosc, y = szerokosc. */
@Component
public class Wgs84CoordinateTransformer implements CoordinateTransformer {

    public static final String EPSG_4326 = "EPSG:4326";

    @Override
    public String targetCrsCode() {
        return EPSG_4326;
    }

    @Override
    public ExportCoordinates transform(BigDecimal latitude, BigDecimal longitude) {
        return new ExportCoordinates(longitude, latitude);
    }
}
