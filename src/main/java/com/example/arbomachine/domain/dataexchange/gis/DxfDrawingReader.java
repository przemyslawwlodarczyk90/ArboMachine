package com.example.arbomachine.domain.dataexchange.gis;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimalny czytnik DXF (ASCII, sekcja ENTITIES) - etap 29. Odczytuje tylko to, czego potrzebuje import drzew
 * i obszarow: CIRCLE / POINT (srodek, promien), TEXT / MTEXT (punkt wstawienia, tresc) i LWPOLYLINE (wierzcholki,
 * zamkniecie), z nazwa warstwy. Bloki (INSERT), DXF binarny i DWG - nieobslugiwane (DO_DOPRECYZOWANIA).
 * Kodowanie: DXF 2007+ to UTF-8; starsze - Windows-1250 (polskie pliki CAD).
 */
public final class DxfDrawingReader {

    public record DxfPoint(String layer, double x, double y, double radius) {}

    /** height - wysokosc tekstu (kod 40), potrzebna do dopasowania opisu do odnosnika. */
    public record DxfText(String layer, double x, double y, double height, String text) {}

    public record DxfPolyline(String layer, List<double[]> vertices, boolean closed) {}

    public record DxfDrawing(List<DxfPoint> points, List<DxfText> texts, List<DxfPolyline> polylines) {}

    private DxfDrawingReader() {
    }

    public static DxfDrawing read(byte[] content) {
        String text = decode(content);
        String[] lines = text.split("\r\n|\n|\r", -1);
        if (lines.length < 4 || !lines[0].trim().equals("0") || !lines[1].trim().equals("SECTION")) {
            throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "dxf");
        }
        List<DxfPoint> points = new ArrayList<>();
        List<DxfText> texts = new ArrayList<>();
        List<DxfPolyline> polylines = new ArrayList<>();
        boolean inEntities = false;
        String entityType = null;
        List<String[]> entityCodes = new ArrayList<>();
        for (int index = 0; index + 1 < lines.length; index += 2) {
            String code = lines[index].trim();
            String value = lines[index + 1];
            if (code.equals("0")) {
                if (inEntities && entityType != null) addEntity(entityType, entityCodes, points, texts, polylines);
                entityType = null;
                entityCodes.clear();
                if (value.trim().equals("ENDSEC")) inEntities = false;
                else if (inEntities) entityType = value.trim();
                continue;
            }
            if (code.equals("2") && !inEntities && entityType == null && value.trim().equals("ENTITIES")) {
                inEntities = true;
                continue;
            }
            if (inEntities && entityType != null) entityCodes.add(new String[]{code, value});
        }
        return new DxfDrawing(points, texts, polylines);
    }

    private static void addEntity(String type, List<String[]> codes, List<DxfPoint> points, List<DxfText> texts,
                                  List<DxfPolyline> polylines) {
        String layer = "";
        Double x = null;
        Double y = null;
        double radius = 0;
        StringBuilder textValue = new StringBuilder();
        List<double[]> vertices = new ArrayList<>();
        int flags = 0;
        try {
            for (String[] pair : codes) {
                switch (pair[0]) {
                    case "8" -> layer = pair[1].trim();
                    case "10" -> {
                        x = Double.parseDouble(pair[1].trim());
                        if (type.equals("LWPOLYLINE")) vertices.add(new double[]{x, Double.NaN});
                    }
                    case "20" -> {
                        y = Double.parseDouble(pair[1].trim());
                        if (type.equals("LWPOLYLINE") && !vertices.isEmpty()) vertices.getLast()[1] = y;
                    }
                    case "40" -> radius = Double.parseDouble(pair[1].trim()); // promien okregu albo wysokosc tekstu
                    case "1", "3" -> textValue.append(pair[1]);
                    case "70" -> flags = Integer.parseInt(pair[1].trim());
                    default -> { /* pozostale kody pomijamy */ }
                }
            }
        } catch (NumberFormatException corruptedNumber) {
            return;
        }
        switch (type) {
            case "CIRCLE", "POINT" -> {
                if (x != null && y != null) points.add(new DxfPoint(layer, x, y, radius));
            }
            case "TEXT", "MTEXT" -> {
                if (x != null && y != null) texts.add(new DxfText(layer, x, y, radius > 0 ? radius : 1, cleanText(textValue.toString())));
            }
            case "LWPOLYLINE" -> {
                List<double[]> complete = vertices.stream().filter(vertex -> !Double.isNaN(vertex[1])).toList();
                if (complete.size() >= 2) polylines.add(new DxfPolyline(layer, complete, (flags & 1) == 1));
            }
            default -> { /* inne obiekty rysunku pomijamy */ }
        }
    }

    /** Usuwa formatowanie MTEXT (\P, {\f...;}) - zostaje sam tekst. */
    private static String cleanText(String raw) {
        return raw.replaceAll("\\\\P", " ").replaceAll("\\\\[A-Za-z][^;]*;", "").replace("{", "").replace("}", "").trim();
    }

    private static String decode(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException notUtf8) {
            return new String(content, Charset.forName("windows-1250"));
        }
    }

    /** Warstwa konczy sie danym przyrostkiem (bez wielkosci liter), np. "Drzewa-Deciduous-Point" -> "-point". */
    static boolean layerEndsWith(String layer, String suffix) {
        return layer.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT));
    }
}
