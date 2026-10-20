package com.example.arbomachine.domain.dataexchange.gis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Etap 31: linie (granice, drogi, sieci) w czytnikach GIS i warstwy podgladu z rysunku DXF. */
class GisLineGeometryTest {

    @Test
    void geoJsonLineStringAndMultiLineStringAreRead() {
        String geoJson = """
                {"type":"FeatureCollection","features":[
                  {"type":"Feature","properties":{"nazwa":"Aleja"},"geometry":{"type":"LineString","coordinates":[[21.0,52.0],[21.001,52.001]]}},
                  {"type":"Feature","properties":{},"geometry":{"type":"MultiLineString","coordinates":[[[21.0,52.0],[21.1,52.1]],[[21.2,52.2],[21.3,52.3]]]}}
                ]}""";
        GisLayer layer = GeoJsonGisReader.read(geoJson.getBytes(StandardCharsets.UTF_8), new ObjectMapper());
        assertThat(layer.features().get(0).geometry()).isInstanceOf(GisFeature.LineString.class);
        assertThat(((GisFeature.LineString) layer.features().get(0).geometry()).points()).hasSize(2);
        assertThat(((GisFeature.MultiLineString) layer.features().get(1).geometry()).lines()).hasSize(2);
        assertThat(GisFeature.firstPositionOf(layer.features().get(1).geometry())).hasValueSatisfying(
                position -> assertThat(position).containsExactly(21.0, 52.0));
    }

    @Test
    void dxfReferenceLayersKeepDrawingLayersAndTurnClosedPolylinesIntoPolygons() {
        String dxf = String.join("\n", "0", "SECTION", "2", "ENTITIES",
                "0", "LINE", "8", "Ogrodzenie", "10", "7500000", "20", "5788000", "11", "7500010", "21", "5788000",
                "0", "LWPOLYLINE", "8", "Dzialki", "70", "1", "10", "7500000", "20", "5788000", "10", "7500010", "20", "5788000",
                "10", "7500010", "20", "5788010",
                "0", "CIRCLE", "8", "Slupy", "10", "7500005", "20", "5788005", "40", "0.3",
                "0", "ENDSEC", "0", "EOF", "");
        DxfDrawingReader.DxfDrawing drawing = DxfDrawingReader.read(dxf.getBytes(StandardCharsets.UTF_8));
        assertThat(drawing.polylines()).hasSize(1).as("LINE nie trafia do polilinii importu drzew");
        assertThat(drawing.lineSegments()).hasSize(1);

        List<GisLayer> layers = DxfLayerExtractor.referenceLayers(drawing);
        assertThat(layers).extracting(GisLayer::name).containsExactly("Slupy", "Dzialki", "Ogrodzenie");
        assertThat(layers.get(0).features().getFirst().geometry()).isInstanceOf(GisFeature.Point.class);
        GisFeature.Polygon parcel = (GisFeature.Polygon) layers.get(1).features().getFirst().geometry();
        assertThat(parcel.rings().getFirst()).hasSize(4).as("pierscien domkniety pierwszym wierzcholkiem");
        assertThat(layers.get(2).features().getFirst().geometry()).isInstanceOf(GisFeature.LineString.class);
    }
}
