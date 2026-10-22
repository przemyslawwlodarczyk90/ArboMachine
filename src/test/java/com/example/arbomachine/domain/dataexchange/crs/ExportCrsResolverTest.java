package com.example.arbomachine.domain.dataexchange.crs;

import com.example.arbomachine.domain.dataexchange.exception.UnsupportedExportCrsException;
import com.example.arbomachine.domain.dataexchange.export.TreeExportData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Etap 34: wybor ukladu eksportu GIS i przeliczenie WGS 84 -> PL-2000 / PL-1992. */
class ExportCrsResolverTest {

    private final ExportCrsResolver resolver = new ExportCrsResolver(new Wgs84CoordinateTransformer());

    private static TreeExportData treeAt(String latitude, String longitude) {
        return new TreeExportData(1, "1", null, null, null, null, null, null, null, null,
                latitude == null ? null : new BigDecimal(latitude), longitude == null ? null : new BigDecimal(longitude),
                null, null, null, null, List.of(), null, List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void missingOrWgs84CodeGivesWgs84AndPolishCodesGivePolishGrid() {
        assertThat(resolver.resolve(null, List.of()).targetCrsCode()).isEqualTo("EPSG:4326");
        assertThat(resolver.resolve(" ", List.of()).targetCrsCode()).isEqualTo("EPSG:4326");
        assertThat(resolver.resolve("epsg:4326", List.of()).targetCrsCode()).isEqualTo("EPSG:4326");
        assertThat(resolver.resolve("EPSG:2180", List.of()).targetCrsCode()).isEqualTo("EPSG:2180");
        for (int epsgCode = 2176; epsgCode <= 2179; epsgCode++) {
            assertThat(resolver.resolve("EPSG:" + epsgCode, List.of()).targetCrsCode()).isEqualTo("EPSG:" + epsgCode);
        }
    }

    @Test
    void automaticPl2000ZoneIsZoneOfMostLocatedTrees() {
        assertThat(resolver.resolve("PL-2000", List.of(treeAt("51.7", "19.4"), treeAt(null, null))).targetCrsCode())
                .isEqualTo("EPSG:2177");
        assertThat(resolver.resolve("pl-2000", List.of(treeAt("51.7", "19.4"), treeAt("50.4", "21.1"), treeAt("50.4", "21.3"), treeAt("-33.8", "-151.2"))).targetCrsCode())
                .isEqualTo("EPSG:2178");
        assertThat(resolver.resolve("PL-2000", List.of()).targetCrsCode()).as("bez drzew - strefa 7").isEqualTo("EPSG:2178");
    }

    @Test
    void unsupportedCrsIsRejected() {
        for (String requested : List.of("EPSG:3857", "EPSG:abc", "PL-1965", "2180")) {
            assertThatThrownBy(() -> resolver.resolve(requested, List.of())).as(requested)
                    .isInstanceOf(UnsupportedExportCrsException.class);
        }
    }

    @Test
    void polishGridTransformGivesEastingNorthingInMetersMatchingImportInverse() {
        // Punkt kontrolny z PolishGridCrsTest (zgodny z PROJ): PL-2000 strefa 7 (7510595.0306, 5585827.1674).
        CoordinateTransformer.ExportCoordinates coordinates = new PolishGridCoordinateTransformer(PolishGridCrs.PL_2000_ZONE_7)
                .transform(new BigDecimal("50.40814910344493"), new BigDecimal("21.149054763122788"));
        assertThat(coordinates.x().doubleValue()).isCloseTo(7510595.0306, within(0.002));
        assertThat(coordinates.y().doubleValue()).isCloseTo(5585827.1674, within(0.002));
        assertThat(coordinates.x().scale()).isEqualTo(3);
        CoordinateTransformer.ExportCoordinates pl1992 = new PolishGridCoordinateTransformer(PolishGridCrs.PL_1992)
                .transform(new BigDecimal("52.365947621"), new BigDecimal("19.000000000"));
        assertThat(pl1992.x().doubleValue()).isCloseTo(500000, within(0.002));
        assertThat(pl1992.y().doubleValue()).isCloseTo(500000, within(0.002));
    }
}
