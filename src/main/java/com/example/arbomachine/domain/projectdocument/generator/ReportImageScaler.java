package com.example.arbomachine.domain.projectdocument.generator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Zmniejsza zdjecie do rozmiaru wystarczajacego w dokumencie A4 i koduje jako JPEG. Backend nie ma miniatur, a pelne
 * zdjecia (do 15 MB) osadzone w PDF dawalyby ogromne pliki. Jedno zdjecie dekodowane naraz; przezroczystosc (PNG)
 * na bialym tle. V1 bez obrotu wg EXIF (docs/DO_DOPRECYZOWANIA.md).
 */
final class ReportImageScaler {

    /** Dluzszy bok zdjecia w dokumencie - ok. 220 dpi na szerokosci strony A4; do weryfikacji z wymaganiami druku. */
    static final int MAXIMUM_IMAGE_SIDE_PX = 1600;
    private static final float JPEG_QUALITY = 0.85f;

    private ReportImageScaler() {
    }

    static byte[] scaledJpegOf(InputStream imageContent) throws IOException {
        BufferedImage original = ImageIO.read(imageContent);
        if (original == null) throw new IOException("Unsupported image format");
        double scale = Math.min(1.0, (double) MAXIMUM_IMAGE_SIDE_PX / Math.max(original.getWidth(), original.getHeight()));
        int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(original, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return jpegBytesOf(scaled);
    }

    private static byte[] jpegBytesOf(BufferedImage image) throws IOException {
        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream jpegBytes = new ByteArrayOutputStream();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(jpegBytes)) {
            jpegWriter.setOutput(output);
            ImageWriteParam writeParameters = jpegWriter.getDefaultWriteParam();
            writeParameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParameters.setCompressionQuality(JPEG_QUALITY);
            jpegWriter.write(null, new IIOImage(image, null, null), writeParameters);
        } finally {
            jpegWriter.dispose();
        }
        return jpegBytes.toByteArray();
    }
}
