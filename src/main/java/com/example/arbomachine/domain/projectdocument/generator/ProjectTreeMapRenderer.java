package com.example.arbomachine.domain.projectdocument.generator;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Etap 41: schemat polozenia drzew do dokumentu PDF (V1) - punkty drzew z numerami, podzialka i strzalka polnocy,
 * bez podkladu mapy (podklad z serwera map wymagalby polaczenia z zewnetrzna usluga i licencji wydruku).
 * Odwzorowanie lokalne: dlugosc geograficzna skalowana cos(szerokosci srodka) - na obszarze inwentaryzacji (do kilku km)
 * blad odleglosci ponizej 1%, wystarczajacy dla podzialki schematu. Legenda jest w HTML dokumentu (tekst do
 * wyszukania w PDF, polskie znaki z czcionki dokumentu), na obrazie tylko numery drzew.
 */
final class ProjectTreeMapRenderer {

    static final int IMAGE_WIDTH_PX = 1600;
    static final int IMAGE_HEIGHT_PX = 1100;
    /** Najmniejszy pokazany obszar (m) - jedno drzewo albo kilka obok siebie nie wypelnia calego obrazu. */
    static final double MINIMUM_EXTENT_M = 40;
    static final Color TREE_COLOR = new Color(0x2E, 0x7D, 0x32);
    static final Color TREE_WITH_OPEN_WORK_COLOR = new Color(0xE6, 0x51, 0x00);

    private static final double EARTH_RADIUS_M = 6_371_000;
    private static final int MARGIN_PX = 70;
    private static final double POINT_RADIUS_PX = 9;
    private static final String FONT_RESOURCE = "/fonts/documents/DejaVuSans.ttf";

    /** Drzewo na schemacie: numer, WGS84 i czy ma zalecenie / prace do wykonania (inny kolor punktu). */
    record MapTree(String label, double latitude, double longitude, boolean hasOpenWork) {}

    /** Obraz PNG i liczba numerow pominietych, bo nachodzilyby na inne (drzewa gesto obok siebie). */
    record RenderedTreeMap(byte[] pngBytes, int skippedLabelCount) {}

    private ProjectTreeMapRenderer() {
    }

    static RenderedTreeMap render(List<MapTree> trees) {
        if (trees.isEmpty()) throw new IllegalArgumentException("No located trees to draw");
        double centerLatitude = trees.stream().mapToDouble(MapTree::latitude).average().orElseThrow();
        double longitudeScale = Math.cos(Math.toRadians(centerLatitude));
        List<double[]> metricPoints = trees.stream()
                .map(tree -> new double[]{
                        Math.toRadians(tree.longitude()) * longitudeScale * EARTH_RADIUS_M,
                        Math.toRadians(tree.latitude()) * EARTH_RADIUS_M})
                .toList();
        Viewport viewport = Viewport.fitting(metricPoints);

        BufferedImage image = new BufferedImage(IMAGE_WIDTH_PX, IMAGE_HEIGHT_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        int skippedLabelCount;
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, IMAGE_WIDTH_PX, IMAGE_HEIGHT_PX);
            graphics.setColor(new Color(0x9C, 0xA3, 0xAF));
            graphics.setStroke(new BasicStroke(2f));
            graphics.drawRect(1, 1, IMAGE_WIDTH_PX - 3, IMAGE_HEIGHT_PX - 3);

            List<double[]> pixelPoints = metricPoints.stream().map(viewport::toPixel).toList();
            for (int index = 0; index < trees.size(); index++) drawPoint(graphics, pixelPoints.get(index), trees.get(index).hasOpenWork());
            skippedLabelCount = drawLabels(graphics, trees, pixelPoints);
            drawScaleBar(graphics, viewport.metersPerPixel());
            drawNorthArrow(graphics);
        } finally {
            graphics.dispose();
        }
        return new RenderedTreeMap(pngBytesOf(image), skippedLabelCount);
    }

    /** Obszar w metrach wpasowany w obraz z marginesem, ta sama skala w obu osiach (os Y w gore). */
    private record Viewport(double centerX, double centerY, double pixelsPerMeter) {

        static Viewport fitting(List<double[]> metricPoints) {
            double minX = metricPoints.stream().mapToDouble(point -> point[0]).min().orElseThrow();
            double maxX = metricPoints.stream().mapToDouble(point -> point[0]).max().orElseThrow();
            double minY = metricPoints.stream().mapToDouble(point -> point[1]).min().orElseThrow();
            double maxY = metricPoints.stream().mapToDouble(point -> point[1]).max().orElseThrow();
            double extentX = Math.max(maxX - minX, MINIMUM_EXTENT_M);
            double extentY = Math.max(maxY - minY, MINIMUM_EXTENT_M);
            double pixelsPerMeter = Math.min((IMAGE_WIDTH_PX - 2.0 * MARGIN_PX) / extentX, (IMAGE_HEIGHT_PX - 2.0 * MARGIN_PX) / extentY);
            return new Viewport((minX + maxX) / 2, (minY + maxY) / 2, pixelsPerMeter);
        }

        double[] toPixel(double[] metricPoint) {
            return new double[]{
                    IMAGE_WIDTH_PX / 2.0 + (metricPoint[0] - centerX) * pixelsPerMeter,
                    IMAGE_HEIGHT_PX / 2.0 - (metricPoint[1] - centerY) * pixelsPerMeter};
        }

        double metersPerPixel() {
            return 1 / pixelsPerMeter;
        }
    }

    private static void drawPoint(Graphics2D graphics, double[] pixel, boolean hasOpenWork) {
        Ellipse2D point = new Ellipse2D.Double(pixel[0] - POINT_RADIUS_PX, pixel[1] - POINT_RADIUS_PX, 2 * POINT_RADIUS_PX, 2 * POINT_RADIUS_PX);
        graphics.setColor(hasOpenWork ? TREE_WITH_OPEN_WORK_COLOR : TREE_COLOR);
        graphics.fill(point);
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(2.5f));
        graphics.draw(point);
    }

    /**
     * Numer przy punkcie: kolejno prawo-gora, prawo-dol, lewo-gora, lewo-dol; gdy kazda pozycja nachodzi na inny numer,
     * punkt albo wychodzi poza obraz - numer pominiety (jest w zestawieniu drzew).
     */
    private static int drawLabels(Graphics2D graphics, List<MapTree> trees, List<double[]> pixelPoints) {
        graphics.setFont(labelFont(trees.size() > 150 ? 15f : 21f));
        FontMetrics metrics = graphics.getFontMetrics();
        List<Rectangle2D> occupied = new ArrayList<>();
        pixelPoints.forEach(pixel -> occupied.add(new Rectangle2D.Double(pixel[0] - POINT_RADIUS_PX, pixel[1] - POINT_RADIUS_PX,
                2 * POINT_RADIUS_PX, 2 * POINT_RADIUS_PX)));
        int skippedLabelCount = 0;
        for (int index = 0; index < trees.size(); index++) {
            String label = trees.get(index).label();
            double[] pixel = pixelPoints.get(index);
            double width = metrics.stringWidth(label);
            double height = metrics.getAscent();
            double gap = POINT_RADIUS_PX + 3;
            double[][] candidates = {
                    {pixel[0] + gap, pixel[1] - gap - height}, {pixel[0] + gap, pixel[1] + gap},
                    {pixel[0] - gap - width, pixel[1] - gap - height}, {pixel[0] - gap - width, pixel[1] + gap}};
            Rectangle2D placed = null;
            for (double[] candidate : candidates) {
                Rectangle2D box = new Rectangle2D.Double(candidate[0], candidate[1], width, height);
                boolean insideImage = box.getMinX() >= 4 && box.getMinY() >= 4
                        && box.getMaxX() <= IMAGE_WIDTH_PX - 4 && box.getMaxY() <= IMAGE_HEIGHT_PX - 4;
                if (insideImage && occupied.stream().noneMatch(other -> other.intersects(box))) {
                    placed = box;
                    break;
                }
            }
            if (placed == null) {
                skippedLabelCount++;
                continue;
            }
            occupied.add(placed);
            drawHaloText(graphics, label, placed.getMinX(), placed.getMinY() + height);
        }
        return skippedLabelCount;
    }

    /** Tekst z biala obwodka - czytelny takze na innych punktach i liniach. */
    private static void drawHaloText(Graphics2D graphics, String text, double x, double baselineY) {
        TextLayout layout = new TextLayout(text, graphics.getFont(), graphics.getFontRenderContext());
        java.awt.Shape outline = layout.getOutline(AffineTransform.getTranslateInstance(x, baselineY));
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(outline);
        graphics.setColor(new Color(0x11, 0x18, 0x27));
        graphics.fill(outline);
    }

    /** Podzialka: najwieksza "okragla" dlugosc (1, 2, 5 x 10^n m) nie dluzsza niz 1/4 szerokosci obrazu. */
    static double scaleBarLengthMeters(double metersPerPixel) {
        double maximumMeters = metersPerPixel * IMAGE_WIDTH_PX / 4;
        double magnitude = Math.pow(10, Math.floor(Math.log10(maximumMeters)));
        for (double step : new double[]{5, 2, 1}) {
            if (step * magnitude <= maximumMeters) return step * magnitude;
        }
        return magnitude;
    }

    private static void drawScaleBar(Graphics2D graphics, double metersPerPixel) {
        double lengthMeters = scaleBarLengthMeters(metersPerPixel);
        int lengthPx = (int) Math.round(lengthMeters / metersPerPixel);
        int x = 30;
        int y = IMAGE_HEIGHT_PX - 32;
        graphics.setColor(Color.WHITE);
        graphics.fillRect(x - 8, y - 44, lengthPx + 16, 54);
        graphics.setColor(new Color(0x11, 0x18, 0x27));
        graphics.fillRect(x, y, lengthPx / 2, 8);
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawRect(x, y, lengthPx, 8);
        graphics.setFont(labelFont(20f));
        String text = (lengthMeters >= 1000 ? formatNumber(lengthMeters / 1000) + " km" : formatNumber(lengthMeters) + " m");
        graphics.drawString(text, x, y - 10);
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value).replace('.', ',');
    }

    private static void drawNorthArrow(Graphics2D graphics) {
        int centerX = IMAGE_WIDTH_PX - 50;
        int top = 26;
        graphics.setColor(new Color(0x11, 0x18, 0x27));
        graphics.fillPolygon(new Polygon(new int[]{centerX, centerX + 14, centerX, centerX - 14}, new int[]{top, top + 46, top + 34, top + 46}, 4));
        graphics.setFont(labelFont(22f));
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString("N", centerX - metrics.stringWidth("N") / 2, top + 76);
    }

    private static Font labelFont(float size) {
        try (InputStream fontStream = Objects.requireNonNull(ProjectTreeMapRenderer.class.getResourceAsStream(FONT_RESOURCE),
                "Missing map font")) {
            return Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(size);
        } catch (IOException | FontFormatException fontError) {
            return new Font(Font.SANS_SERIF, Font.PLAIN, Math.round(size));
        }
    }

    private static byte[] pngBytesOf(BufferedImage image) {
        try {
            ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
            ImageIO.write(image, "png", pngBytes);
            return pngBytes.toByteArray();
        } catch (IOException encodingError) {
            throw new IllegalStateException("Could not encode tree map", encodingError);
        }
    }
}
