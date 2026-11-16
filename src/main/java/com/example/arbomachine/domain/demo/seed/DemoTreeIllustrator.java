package com.example.arbomachine.domain.demo.seed;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Random;

/**
 * Ilustracje do dokumentacji fotograficznej trybu demo - rysowane w kodzie (Java2D), wiec sa w 100% fikcyjne i bez praw
 * autorskich osob trzecich. Bez tekstu w obrazie (nie wymaga fontow w obrazie Docker). Deterministyczne dla ziarna.
 */
final class DemoTreeIllustrator {

    static final int WIDTH = 1200;
    static final int HEIGHT = 900;

    /** Ujecie zdjecia. */
    enum Shot { WHOLE_TREE, TRUNK_DETAIL, CROWN_DETAIL }

    /** Element adnotacji narysowany na zdjeciu (wspolrzedne znormalizowane 0..1, jak w dokumencie adnotacji). */
    record AnnotationStroke(String type, List<double[]> points, Color color) {}

    private DemoTreeIllustrator() {
    }

    static BufferedImage draw(Shot shot, boolean conifer, boolean autumn, long seed) {
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (shot) {
            case WHOLE_TREE -> drawWholeTree(g, random, conifer, autumn);
            case TRUNK_DETAIL -> drawTrunkDetail(g, random);
            case CROWN_DETAIL -> drawCrownDetail(g, random, conifer, autumn);
        }
        g.dispose();
        return image;
    }

    private static void drawWholeTree(Graphics2D g, Random random, boolean conifer, boolean autumn) {
        g.setPaint(new GradientPaint(0, 0, new Color(150, 195, 235), 0, HEIGHT * 0.7f, new Color(222, 236, 245)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // dalszy plan - pas krzewow
        g.setColor(new Color(120, 160, 110));
        for (int x = -40; x < WIDTH + 40; x += 70 + random.nextInt(40)) {
            int r = 50 + random.nextInt(40);
            g.fill(new Ellipse2D.Double(x, HEIGHT * 0.68 - r, r * 2.2, r * 1.4));
        }
        g.setPaint(new GradientPaint(0, HEIGHT * 0.72f, new Color(118, 158, 82), 0, HEIGHT, new Color(84, 122, 58)));
        g.fillRect(0, (int) (HEIGHT * 0.72), WIDTH, HEIGHT);
        // sciezka
        g.setColor(new Color(196, 180, 150));
        g.fill(new Polygon(new int[]{WIDTH / 2 + 160, WIDTH / 2 + 230, WIDTH, WIDTH}, new int[]{(int) (HEIGHT * 0.72), (int) (HEIGHT * 0.72), (int) (HEIGHT * 0.86), HEIGHT}, 4));

        int centerX = WIDTH / 2 - 60 + random.nextInt(120);
        int groundY = (int) (HEIGHT * 0.82);
        int trunkWidth = 46 + random.nextInt(40);
        Color bark = new Color(96 + random.nextInt(20), 72 + random.nextInt(15), 52);
        g.setColor(new Color(0, 0, 0, 40));
        g.fill(new Ellipse2D.Double(centerX - 220, groundY - 18, 440, 46));
        if (conifer) {
            g.setColor(bark);
            g.fillRect(centerX - trunkWidth / 3, groundY - 520, trunkWidth * 2 / 3, 520);
            Color needles = new Color(36 + random.nextInt(20), 88 + random.nextInt(25), 52 + random.nextInt(20));
            for (int layer = 0; layer < 7; layer++) {
                int top = 90 + layer * 78;
                int halfWidth = 70 + layer * 34 + random.nextInt(18);
                g.setColor(layer % 2 == 0 ? needles : needles.darker());
                g.fill(new Polygon(new int[]{centerX, centerX - halfWidth, centerX + halfWidth},
                        new int[]{top, top + 170, top + 170}, 3));
            }
        } else {
            drawDeciduousTrunk(g, centerX, groundY, trunkWidth, bark);
            Color[] leaves = autumn
                    ? new Color[]{new Color(214, 150, 40), new Color(190, 110, 36), new Color(228, 186, 70), new Color(160, 120, 50)}
                    : new Color[]{new Color(70, 128, 58), new Color(92, 150, 66), new Color(58, 110, 50), new Color(110, 162, 80)};
            double crownCenterY = groundY - 470;
            for (int blob = 0; blob < 46; blob++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = Math.sqrt(random.nextDouble());
                double x = centerX + Math.cos(angle) * distance * 270;
                double y = crownCenterY + Math.sin(angle) * distance * 200;
                double radius = 70 + random.nextInt(60);
                g.setColor(leaves[random.nextInt(leaves.length)]);
                g.fill(new Ellipse2D.Double(x - radius, y - radius * 0.85, radius * 2, radius * 1.7));
            }
        }
    }

    private static void drawDeciduousTrunk(Graphics2D g, int centerX, int groundY, int trunkWidth, Color bark) {
        g.setColor(bark);
        Path2D trunk = new Path2D.Double();
        trunk.moveTo(centerX - trunkWidth * 0.9, groundY);
        trunk.curveTo(centerX - trunkWidth * 0.5, groundY - 120, centerX - trunkWidth * 0.45, groundY - 260, centerX - trunkWidth * 0.35, groundY - 420);
        trunk.lineTo(centerX + trunkWidth * 0.35, groundY - 420);
        trunk.curveTo(centerX + trunkWidth * 0.45, groundY - 260, centerX + trunkWidth * 0.5, groundY - 120, centerX + trunkWidth * 0.9, groundY);
        trunk.closePath();
        g.fill(trunk);
        g.setStroke(new BasicStroke(trunkWidth * 0.32f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(centerX, groundY - 380, centerX - 170, groundY - 560);
        g.drawLine(centerX, groundY - 400, centerX + 180, groundY - 580);
        g.drawLine(centerX, groundY - 420, centerX + 20, groundY - 640);
    }

    private static void drawTrunkDetail(Graphics2D g, Random random) {
        g.setColor(new Color(108, 132, 92));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Color bark = new Color(104 + random.nextInt(20), 84 + random.nextInt(14), 64);
        g.setColor(bark);
        g.fillRect(WIDTH / 2 - 330, 0, 660, HEIGHT);
        // spekania kory
        g.setColor(bark.darker());
        g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int crack = 0; crack < 26; crack++) {
            int x = WIDTH / 2 - 310 + random.nextInt(620);
            int y = random.nextInt(HEIGHT);
            Path2D line = new Path2D.Double();
            line.moveTo(x, y);
            line.curveTo(x + random.nextInt(30) - 15, y + 70, x + random.nextInt(30) - 15, y + 140, x + random.nextInt(24) - 12, y + 210);
            g.draw(line);
        }
        // ubytek / dziupla
        g.setColor(new Color(46, 32, 22));
        g.fill(new Ellipse2D.Double(WIDTH / 2.0 - 70, HEIGHT * 0.42, 130, 210));
        // owocnik grzyba
        g.setColor(new Color(214, 176, 112));
        g.fill(new Ellipse2D.Double(WIDTH / 2.0 + 120, HEIGHT * 0.68, 170, 70));
        g.setColor(new Color(170, 130, 80));
        g.fill(new Ellipse2D.Double(WIDTH / 2.0 + 150, HEIGHT * 0.70, 110, 32));
    }

    private static void drawCrownDetail(Graphics2D g, Random random, boolean conifer, boolean autumn) {
        g.setPaint(new GradientPaint(0, 0, new Color(140, 188, 232), 0, HEIGHT, new Color(214, 232, 246)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        Color branch = new Color(88, 66, 48);
        g.setColor(branch);
        g.setStroke(new BasicStroke(34f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(-40, HEIGHT - 60, WIDTH / 2, HEIGHT / 2);
        g.setStroke(new BasicStroke(18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(WIDTH / 2, HEIGHT / 2, WIDTH - 140, 160);
        g.drawLine(WIDTH / 2, HEIGHT / 2, WIDTH / 2 + 60, 60);
        // suchy konar (posusz)
        g.setColor(new Color(150, 140, 128));
        g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(WIDTH / 2 - 120, HEIGHT / 2 + 90, 180, 240);
        g.drawLine(300, 300, 260, 170);
        Color[] leaves = conifer
                ? new Color[]{new Color(40, 92, 56), new Color(30, 76, 46)}
                : autumn ? new Color[]{new Color(214, 150, 40), new Color(190, 110, 36)}
                : new Color[]{new Color(76, 134, 60), new Color(98, 156, 70), new Color(62, 116, 52)};
        for (int blob = 0; blob < 34; blob++) {
            double x = WIDTH / 2.0 + random.nextInt(WIDTH / 2) - 40;
            double y = random.nextInt(HEIGHT / 2 + 80);
            double radius = 50 + random.nextInt(50);
            g.setColor(leaves[random.nextInt(leaves.length)]);
            g.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 1.6));
        }
    }

    /** Adnotacje pasujace do ujecia (strzalka i obrys w miejscu ubytku / posuszu). */
    static List<AnnotationStroke> annotationFor(Shot shot) {
        return switch (shot) {
            case TRUNK_DETAIL -> List.of(
                    new AnnotationStroke("ARROW", List.of(new double[]{0.18, 0.30}, new double[]{0.42, 0.50}), Color.RED),
                    new AnnotationStroke("FREEHAND", circle(0.494, 0.58, 0.085, 0.15), Color.RED),
                    new AnnotationStroke("FREEHAND", circle(0.62, 0.795, 0.09, 0.07), Color.YELLOW));
            case CROWN_DETAIL -> List.of(
                    new AnnotationStroke("ARROW", List.of(new double[]{0.10, 0.80}, new double[]{0.20, 0.38}), Color.YELLOW),
                    new AnnotationStroke("LINE", List.of(new double[]{0.13, 0.22}, new double[]{0.42, 0.62}), Color.RED));
            case WHOLE_TREE -> List.of(
                    new AnnotationStroke("ARROW", List.of(new double[]{0.85, 0.20}, new double[]{0.62, 0.34}), Color.RED),
                    new AnnotationStroke("LINE", List.of(new double[]{0.30, 0.86}, new double[]{0.70, 0.86}), Color.YELLOW));
        };
    }

    private static List<double[]> circle(double centerX, double centerY, double radiusX, double radiusY) {
        double[][] points = new double[25][];
        for (int i = 0; i < points.length; i++) {
            double angle = 2 * Math.PI * i / (points.length - 1);
            points[i] = new double[]{clamp(centerX + Math.cos(angle) * radiusX), clamp(centerY + Math.sin(angle) * radiusY)};
        }
        return List.of(points);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    /** Zdjecie z narysowanymi adnotacjami (splaszczony PNG - tak jak zapisuje go edytor adnotacji). */
    static BufferedImage withAnnotations(BufferedImage original, List<AnnotationStroke> strokes) {
        BufferedImage rendered = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rendered.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float strokeWidth = Math.max(original.getWidth(), original.getHeight()) * 0.006f;
        g.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (AnnotationStroke stroke : strokes) {
            g.setColor(stroke.color());
            Path2D path = new Path2D.Double();
            double[] first = stroke.points().getFirst();
            path.moveTo(first[0] * original.getWidth(), first[1] * original.getHeight());
            for (double[] point : stroke.points().subList(1, stroke.points().size())) {
                path.lineTo(point[0] * original.getWidth(), point[1] * original.getHeight());
            }
            g.draw(path);
            if ("ARROW".equals(stroke.type())) drawArrowHead(g, stroke.points(), original.getWidth(), original.getHeight(), strokeWidth);
        }
        g.dispose();
        return rendered;
    }

    private static void drawArrowHead(Graphics2D g, List<double[]> points, int width, int height, float strokeWidth) {
        double x1 = points.get(0)[0] * width, y1 = points.get(0)[1] * height;
        double x2 = points.get(1)[0] * width, y2 = points.get(1)[1] * height;
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double headLength = strokeWidth * 5;
        Path2D head = new Path2D.Double();
        head.moveTo(x2 - headLength * Math.cos(angle - Math.PI / 7), y2 - headLength * Math.sin(angle - Math.PI / 7));
        head.lineTo(x2, y2);
        head.lineTo(x2 - headLength * Math.cos(angle + Math.PI / 7), y2 - headLength * Math.sin(angle + Math.PI / 7));
        g.draw(head);
    }

    static byte[] toJpeg(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam parameters = writer.getDefaultWriteParam();
            parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parameters.setCompressionQuality(0.82f);
            writer.setOutput(imageOutput);
            writer.write(null, new IIOImage(image, null, null), parameters);
            writer.dispose();
            imageOutput.flush();
            return output.toByteArray();
        } catch (IOException encodingError) {
            throw new UncheckedIOException(encodingError);
        }
    }

    static byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException encodingError) {
            throw new UncheckedIOException(encodingError);
        }
    }
}
