package com.example.arbomachine.domain.projectdocument.generator;

import com.example.arbomachine.domain.projectdocument.generator.ProjectTreeMapRenderer.MapTree;
import com.example.arbomachine.domain.projectdocument.generator.ProjectTreeMapRenderer.RenderedTreeMap;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Etap 41: schemat polozenia drzew do dokumentu PDF. */
class ProjectTreeMapRendererTest {

    private static BufferedImage imageOf(RenderedTreeMap map) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(map.pngBytes()));
    }

    private static boolean containsColor(BufferedImage image, int rgb) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == rgb) return true;
            }
        }
        return false;
    }

    @Test
    void drawsPngWithTreePointsInBothColors() throws IOException {
        RenderedTreeMap map = ProjectTreeMapRenderer.render(List.of(
                new MapTree("1", 52.2297, 21.0122, false),
                new MapTree("2", 52.2301, 21.0130, true)));

        BufferedImage image = imageOf(map);
        assertThat(image.getWidth()).isEqualTo(ProjectTreeMapRenderer.IMAGE_WIDTH_PX);
        assertThat(image.getHeight()).isEqualTo(ProjectTreeMapRenderer.IMAGE_HEIGHT_PX);
        assertThat(containsColor(image, ProjectTreeMapRenderer.TREE_COLOR.getRGB() & 0xFFFFFF)).isTrue();
        assertThat(containsColor(image, ProjectTreeMapRenderer.TREE_WITH_OPEN_WORK_COLOR.getRGB() & 0xFFFFFF)).isTrue();
        assertThat(map.skippedLabelCount()).isZero();
    }

    @Test
    void singleTreeIsDrawnWithMinimumExtentInsteadOfDividingByZero() throws IOException {
        RenderedTreeMap map = ProjectTreeMapRenderer.render(List.of(new MapTree("A-12", 50.0614, 19.9366, false)));

        BufferedImage image = imageOf(map);
        int center = image.getRGB(ProjectTreeMapRenderer.IMAGE_WIDTH_PX / 2, ProjectTreeMapRenderer.IMAGE_HEIGHT_PX / 2) & 0xFFFFFF;
        assertThat(center).as("jedyne drzewo na srodku obrazu").isEqualTo(ProjectTreeMapRenderer.TREE_COLOR.getRGB() & 0xFFFFFF);
    }

    @Test
    void labelsOfTreesStandingOnTopOfEachOtherAreSkippedNotOverlapped() {
        List<MapTree> trees = new ArrayList<>();
        for (int index = 0; index < 10; index++) trees.add(new MapTree(String.valueOf(index + 1), 52.2297, 21.0122, false));
        trees.add(new MapTree("99", 52.2310, 21.0150, false));

        RenderedTreeMap map = ProjectTreeMapRenderer.render(trees);

        assertThat(map.skippedLabelCount()).as("w jednym punkcie mieszcza sie najwyzej 4 numery").isEqualTo(6);
    }

    @Test
    void scaleBarUsesRoundLengthNotLongerThanQuarterOfImage() {
        assertThat(ProjectTreeMapRenderer.scaleBarLengthMeters(0.05)).isEqualTo(20);
        assertThat(ProjectTreeMapRenderer.scaleBarLengthMeters(0.1)).isEqualTo(20);
        assertThat(ProjectTreeMapRenderer.scaleBarLengthMeters(0.5)).isEqualTo(200);
        assertThat(ProjectTreeMapRenderer.scaleBarLengthMeters(3)).isEqualTo(1000);
    }

    @Test
    void emptyListIsRejected() {
        assertThatThrownBy(() -> ProjectTreeMapRenderer.render(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
