package com.example.arbomachine.domain.treestand.geometry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Geometria grupy drzew: walidacja GeoJSON, zamykanie pierscienia, pole (porownane z PROJ/geod) i punkt w obszarze. */
class StandPolygonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private StandPolygon polygon(String geometryJson) throws Exception {
        return StandPolygon.fromGeoJson(objectMapper.readTree(geometryJson));
    }

    @Test
    void areaOfSmallSquareMatchesGeodesicValue() throws Exception {
        // 0.001 x 0.001 stopnia przy 50.4 N: ok. 111.2 m x 71.0 m - geodezyjnie (PROJ) 7 907 m2
        StandPolygon square = polygon("{\"type\":\"Polygon\",\"coordinates\":[[[21.149,50.408],[21.150,50.408],[21.150,50.409],[21.149,50.409],[21.149,50.408]]]}");
        assertThat(square.areaSquareMeters()).isCloseTo(7907, within(40.0));
        assertThat(square.contains(21.1495, 50.4085)).isTrue();
        assertThat(square.contains(21.1505, 50.4085)).isFalse();
    }

    @Test
    void openRingIsClosedAndHoleIsExcluded() throws Exception {
        StandPolygon withHole = polygon("{\"type\":\"Polygon\",\"coordinates\":["
                + "[[21.149,50.408],[21.150,50.408],[21.150,50.409],[21.149,50.409]],"
                + "[[21.1494,50.4084],[21.1496,50.4084],[21.1496,50.4086],[21.1494,50.4086],[21.1494,50.4084]]]}");
        assertThat(withHole.rings().getFirst()).hasSize(5);
        assertThat(withHole.contains(21.1495, 50.4085)).as("punkt w dziurze").isFalse();
        assertThat(withHole.contains(21.1491, 50.4081)).isTrue();
        assertThat(withHole.areaSquareMeters()).isLessThan(7907).isGreaterThan(7500);
        String roundTrip = withHole.toGeoJson(objectMapper).toString();
        assertThat(StandPolygon.fromGeoJsonText(roundTrip, objectMapper).vertexCount()).isEqualTo(10);
    }

    @Test
    void invalidGeometriesAreRejected() {
        for (String invalid : List.of(
                "{\"type\":\"Point\",\"coordinates\":[21,50]}",
                "{\"type\":\"Polygon\",\"coordinates\":[[[21,50],[21.1,50],[21,50]]]}",
                "{\"type\":\"Polygon\",\"coordinates\":[[[7510595,5585827],[7510600,5585827],[7510600,5585830],[7510595,5585827]]]}",
                "{\"type\":\"Polygon\",\"coordinates\":[[[21,50],[21,50],[21,50],[21,50]]]}",
                "{\"type\":\"Polygon\",\"coordinates\":\"x\"}",
                "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[21,50],[21.1,50],[21.1,50.1],[21,50]]],[[[22,50],[22.1,50],[22.1,50.1],[22,50]]]]}")) {
            assertThatThrownBy(() -> polygon(invalid)).as(invalid).isInstanceOf(InvalidStandGeometryException.class);
        }
        assertThatThrownBy(() -> StandPolygon.fromGeoJson(null)).isInstanceOf(InvalidStandGeometryException.class);
    }
}
