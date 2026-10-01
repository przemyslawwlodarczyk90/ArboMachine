package com.example.arbomachine.domain.projectdocument.generator;

import com.example.arbomachine.domain.projectdocument.data.DocumentImageContent;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentPhotoData;
import com.openhtmltopdf.extend.FSStream;
import com.openhtmltopdf.extend.FSStreamFactory;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * "Dokument projektu" V1: szablon Thymeleaf templates/documents/project-report.html -> HTML -> PDF (openhtmltopdf).
 * Wyglad i tresc zmienia sie w szablonie (HTML/CSS), nie w kodzie. Czcionka DejaVu Sans z zasobow aplikacji
 * (polskie znaki niezaleznie od systemu). Zdjecia sa ladowane leniwie przez wlasny protokol "report-image://N",
 * zmniejszane (ReportImageScaler) i osadzane jako JPEG; zdjecie, ktorego nie da sie odczytac, jest pomijane
 * (podpis zostaje) zamiast przerywac generowanie calego dokumentu.
 */
@Component
public class ProjectReportPdfGenerator implements DocumentGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectReportPdfGenerator.class);

    static final String TEMPLATE_NAME = "documents/project-report";
    static final String IMAGE_PROTOCOL = "report-image";
    private static final String FONT_FAMILY = "DejaVu Sans";
    private static final String FONTS_DIRECTORY = "/fonts/documents/";

    private final TemplateEngine templateEngine;

    public ProjectReportPdfGenerator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public GeneratedDocument generate(ProjectDocumentData data) {
        Context templateVariables = new Context();
        templateVariables.setVariable("document", data);
        templateVariables.setVariable("format", new ProjectReportFormatter());
        templateVariables.setVariable("imageProtocol", IMAGE_PROTOCOL);
        String html = templateEngine.process(TEMPLATE_NAME, templateVariables);

        Map<String, DocumentImageContent> imageContentByNumber = new HashMap<>();
        data.trees().forEach(tree -> tree.photos().stream().filter(ProjectDocumentPhotoData::embeddable)
                .forEach(photo -> imageContentByNumber.put(String.valueOf(photo.documentPhotoNumber()), photo.content())));

        ByteArrayOutputStream pdfBytes = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            useFont(builder, "DejaVuSans.ttf", 400, FontStyle.NORMAL);
            useFont(builder, "DejaVuSans-Bold.ttf", 700, FontStyle.NORMAL);
            useFont(builder, "DejaVuSans-Oblique.ttf", 400, FontStyle.ITALIC);
            builder.useProtocolsStreamImplementation(new ReportImageStreamFactory(imageContentByNumber), IMAGE_PROTOCOL);
            builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(html)), "about:blank");
            builder.toStream(pdfBytes);
            builder.run();
        } catch (IOException | RuntimeException renderingError) {
            throw new DocumentGenerationException("Could not generate project document", renderingError);
        }
        return new GeneratedDocument(pdfBytes.toByteArray(), "application/pdf", fileNameOf(data));
    }

    /** Nazwa pliku tylko z id projektu - bez nazwy wpisanej przez uzytkownika (nie trzeba jej sanityzowac). */
    static String fileNameOf(ProjectDocumentData data) {
        return "arbomachine-projekt-" + data.projectId() + ".pdf";
    }

    private static void useFont(PdfRendererBuilder builder, String fileName, int weight, FontStyle style) {
        builder.useFont(() -> Objects.requireNonNull(ProjectReportPdfGenerator.class.getResourceAsStream(FONTS_DIRECTORY + fileName),
                "Missing document font " + fileName), FONT_FAMILY, weight, style, true);
    }

    /**
     * report-image://N -> zmniejszony JPEG zdjecia nr N; brak albo blad -> obraz pominiety (log), dokument powstaje.
     * Renderer pyta o ten sam adres kilka razy (uklad, rysowanie) - wynik jest zapamietywany, wiec kazde zdjecie
     * jest czytane i zmniejszane tylko raz (pusta tablica = zdjecia nie da sie osadzic).
     */
    private record ReportImageStreamFactory(Map<String, DocumentImageContent> imageContentByNumber,
                                            Map<String, byte[]> scaledImageByNumber) implements FSStreamFactory {

        private static final byte[] UNAVAILABLE_IMAGE = new byte[0];

        ReportImageStreamFactory(Map<String, DocumentImageContent> imageContentByNumber) {
            this(imageContentByNumber, new HashMap<>());
        }

        @Override
        public FSStream getUrl(String url) {
            String photoNumber = url.substring(url.indexOf("://") + 3).replaceAll("/+$", "");
            byte[] cachedImage = scaledImageByNumber.computeIfAbsent(photoNumber, number -> {
                byte[] loadedImage = loadScaledImage(number);
                return loadedImage == null ? UNAVAILABLE_IMAGE : loadedImage;
            });
            byte[] imageBytes = cachedImage.length == 0 ? null : cachedImage;
            return new FSStream() {
                @Override
                public InputStream getStream() {
                    return imageBytes == null ? null : new ByteArrayInputStream(imageBytes);
                }

                @Override
                public Reader getReader() {
                    return null;
                }
            };
        }

        private byte[] loadScaledImage(String photoNumber) {
            DocumentImageContent content = imageContentByNumber.get(photoNumber);
            if (content == null) return null;
            try (InputStream imageStream = content.openStream()) {
                return ReportImageScaler.scaledJpegOf(imageStream);
            } catch (IOException | RuntimeException imageError) {
                LOGGER.warn("Photo {} could not be embedded in project document", photoNumber, imageError);
                return null;
            }
        }
    }
}
