package com.example.arbomachine.domain.dataexchange.crs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** PL-2000 / PL-1992 -> WGS 84; wartosci wzorcowe z PROJ (pyproj 3.x, always_xy) - dokladnosc 1e-8 stopnia (~1 mm). */
class PolishGridCrsTest {

    private static void assertWgs84(PolishGridCrs crs, double x, double y, double expectedLongitude, double expectedLatitude) {
        double[] lonLat = crs.toWgs84(x, y);
        assertThat(lonLat[0]).as(crs + " dlugosc").isCloseTo(expectedLongitude, within(1e-8));
        assertThat(lonLat[1]).as(crs + " szerokosc").isCloseTo(expectedLatitude, within(1e-8));
    }

    @Test
    void polishGridsMatchProjReferencePoints() {
        assertWgs84(PolishGridCrs.PL_2000_ZONE_7, 7510595.0306, 5585827.1674, 21.149054763122788, 50.40814910344493);
        assertWgs84(PolishGridCrs.PL_2000_ZONE_5, 5500000, 5800000, 15.000000000, 52.333449373);
        assertWgs84(PolishGridCrs.PL_2000_ZONE_6, 6500000, 5700000, 18.000000000, 51.434626100);
        assertWgs84(PolishGridCrs.PL_2000_ZONE_8, 8400000, 5900000, 22.502633756, 53.222729043);
        assertWgs84(PolishGridCrs.PL_1992, 500000, 500000, 19.000000000, 52.365947621);
        assertWgs84(PolishGridCrs.PL_1992, 300000, 650000, 15.970972808, 53.676446348);
        assertWgs84(PolishGridCrs.WGS84, 21.1, 50.4, 21.1, 50.4);
    }

    @Test
    void coordinateSystemIsGuessedFromCoordinateMagnitude() {
        assertThat(PolishGridCrs.guessFromCoordinates(7510595, 5585827)).contains(PolishGridCrs.PL_2000_ZONE_7);
        assertThat(PolishGridCrs.guessFromCoordinates(5500000, 5800000)).contains(PolishGridCrs.PL_2000_ZONE_5);
        assertThat(PolishGridCrs.guessFromCoordinates(8400000, 5900000)).contains(PolishGridCrs.PL_2000_ZONE_8);
        assertThat(PolishGridCrs.guessFromCoordinates(500000, 500000)).contains(PolishGridCrs.PL_1992);
        assertThat(PolishGridCrs.guessFromCoordinates(21.1, 50.4)).contains(PolishGridCrs.WGS84);
        assertThat(PolishGridCrs.guessFromCoordinates(12, 3_000_000)).isEmpty();
        assertThat(PolishGridCrs.fromEpsgCode(2180)).contains(PolishGridCrs.PL_1992);
    }
}
