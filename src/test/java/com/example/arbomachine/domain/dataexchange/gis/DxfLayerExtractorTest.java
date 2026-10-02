package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfDrawing;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfPoint;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfPolyline;
import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader.DxfText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Dopasowanie numerow drzew do punktow przez odnosniki - takze gdy opisy sasiednich drzew leza blisko siebie. */
class DxfLayerExtractorTest {

    private static List<String> numbersOf(GisLayer layer) {
        return layer.features().stream().map(feature -> feature.attributes().get("inventoryNumber")).toList();
    }

    @Test
    void labelsAreMatchedByLeaderDirectionAndBaselineEvenWhenCloserTextBelongsToNeighbour() {
        DxfDrawing drawing = new DxfDrawing(
                List.of(new DxfPoint("Inw-Deciduous-Point", 0, 0, 0.2), new DxfPoint("Inw-Coniferous-Point", 5, 0.2, 0.2)),
                // "1" lezy tez blisko konca odnosnika B (1,7 j.) - rozstrzyga linia bazowa i kierunek odnosnika (B wskazuje w lewo).
                List.of(new DxfText("Inw-Label", 3.5, 2.5, 1, "1"), new DxfText("Inw-Label", 0.8, 2.8, 1, "22")),
                List.of(new DxfPolyline("Inw-Label", List.of(new double[]{0, 0}, new double[]{3, 3}, new double[]{3.5, 3}), false),
                        new DxfPolyline("Inw-Label", List.of(new double[]{5, 0.2}, new double[]{2.5, 3.3}, new double[]{2, 3.3}), false)));
        assertThat(numbersOf(DxfLayerExtractor.treeLayer(drawing))).containsExactly("1", "22");
    }

    @Test
    void pointWithoutLeaderTakesNearestFreeLabelAndAreasComeFromAreaLayers() {
        DxfDrawing drawing = new DxfDrawing(
                List.of(new DxfPoint("X-Deciduous-Point", 100, 100, 0.2), new DxfPoint("X-Shrub", 50, 50, 0.5)),
                List.of(new DxfText("X-Label", 102, 101, 1, "7"), new DxfText("X-Label", 200, 200, 1, "daleko")),
                List.of(new DxfPolyline("X-Area", List.of(new double[]{0, 0}, new double[]{10, 0}, new double[]{10, 10}), false),
                        new DxfPolyline("X-Label", List.of(new double[]{1, 1}, new double[]{2, 2}), false)));
        assertThat(numbersOf(DxfLayerExtractor.treeLayer(drawing))).containsExactly("7");
        assertThat(DxfLayerExtractor.standLayer(drawing).features()).hasSize(1);
    }
}
