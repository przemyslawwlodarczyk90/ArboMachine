package com.example.arbomachine.domain.dataexchange.gis;

import com.example.arbomachine.domain.dataexchange.crs.PolishGridCrs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Shapefile spakowany do ZIP (etap 29): kazdy komplet .shp + .dbf (+ .prj, .cpg) to jedna warstwa. Obslugiwane typy:
 * Point / PointZ / PointM i Polygon / PolygonZ / PolygonM (wiele obrysow = MultiPolygon, dziury przypisane do obrysu).
 * Inne typy -> obiekty Unsupported. Kodowanie DBF: .cpg, potem UTF-8, w ostatecznosci Windows-1250. Bez bibliotek.
 */
public final class ShapefileZipReader {

    /** Ochrona przed "bomba zip" - rozpakowany komplet nie wiekszy niz to. */
    private static final long MAXIMUM_UNPACKED_BYTES = 100L * 1024 * 1024;

    private ShapefileZipReader() {
    }

    public static List<GisLayer> read(byte[] zipContent) {
        Map<String, Map<String, byte[]>> filesByLayer = new TreeMap<>();
        long unpacked = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                String fileName = entry.getName().replace('\\', '/');
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                int dot = fileName.lastIndexOf('.');
                if (dot <= 0 || fileName.startsWith("._")) continue;
                String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (!List.of("shp", "dbf", "prj", "cpg").contains(extension)) continue;
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) > 0) {
                    unpacked += read;
                    if (unpacked > MAXIMUM_UNPACKED_BYTES) throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "zip too large");
                    content.write(buffer, 0, read);
                }
                filesByLayer.computeIfAbsent(fileName.substring(0, dot), key -> new LinkedHashMap<>()).put(extension, content.toByteArray());
            }
        } catch (IOException | IllegalArgumentException invalidZip) {
            throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "zip");
        }
        List<GisLayer> layers = new ArrayList<>();
        filesByLayer.forEach((layerName, files) -> {
            if (files.containsKey("shp")) layers.add(readLayer(layerName, files));
        });
        if (layers.isEmpty()) throw new GisFileException(GisFileException.Reason.NO_SHAPEFILE_IN_ZIP, null);
        return layers;
    }

    private static GisLayer readLayer(String layerName, Map<String, byte[]> files) {
        List<GisFeature.Geometry> geometries = readGeometries(files.get("shp"));
        List<Map<String, String>> attributes = files.containsKey("dbf")
                ? readDbf(files.get("dbf"), charsetOf(files.get("cpg"))) : List.of();
        List<GisFeature> features = new ArrayList<>();
        for (int index = 0; index < geometries.size(); index++) {
            features.add(new GisFeature(index + 1, geometries.get(index), index < attributes.size() ? attributes.get(index) : Map.of()));
        }
        if (!files.containsKey("prj")) return GisLayer.of(layerName, features, Optional.empty());
        ProjectionFileReader.ProjectionResult projection = ProjectionFileReader.read(new String(files.get("prj"), StandardCharsets.UTF_8));
        return projection.crs().isPresent() ? GisLayer.of(layerName, features, projection.crs())
                : new GisLayer(layerName, features, Optional.empty(), projection.name());
    }

    // ---------- .shp ----------

    private static List<GisFeature.Geometry> readGeometries(byte[] shp) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(shp);
            if (buffer.order(ByteOrder.BIG_ENDIAN).getInt(0) != 9994) throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "shp");
            buffer.position(100);
            List<GisFeature.Geometry> geometries = new ArrayList<>();
            while (buffer.remaining() >= 8) {
                buffer.order(ByteOrder.BIG_ENDIAN);
                buffer.getInt();
                int contentLengthBytes = buffer.getInt() * 2;
                int contentStart = buffer.position();
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                int shapeType = buffer.getInt();
                geometries.add(switch (shapeType) {
                    case 0 -> null;
                    case 1, 11, 21 -> new GisFeature.Point(buffer.getDouble(), buffer.getDouble());
                    case 5, 15, 25 -> readPolygon(buffer);
                    case 3, 13, 23 -> readPolyline(buffer);
                    default -> new GisFeature.Unsupported(shapeTypeName(shapeType));
                });
                buffer.position(contentStart + contentLengthBytes);
            }
            return geometries;
        } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException | IndexOutOfBoundsException corrupted) {
            throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "shp");
        }
    }

    /** PolyLine: te same czesci co wielokat, ale kazda czesc to osobna linia. */
    private static GisFeature.Geometry readPolyline(ByteBuffer buffer) {
        buffer.position(buffer.position() + 32);
        int partCount = buffer.getInt();
        int pointCount = buffer.getInt();
        int[] partStarts = new int[partCount];
        for (int part = 0; part < partCount; part++) partStarts[part] = buffer.getInt();
        double[][] points = new double[pointCount][];
        for (int point = 0; point < pointCount; point++) points[point] = new double[]{buffer.getDouble(), buffer.getDouble()};
        List<GisFeature.LineString> lines = new ArrayList<>();
        for (int part = 0; part < partCount; part++) {
            int end = part + 1 < partCount ? partStarts[part + 1] : pointCount;
            List<double[]> line = new ArrayList<>();
            for (int point = partStarts[part]; point < end; point++) line.add(points[point]);
            lines.add(new GisFeature.LineString(line));
        }
        return lines.size() == 1 ? lines.getFirst() : new GisFeature.MultiLineString(lines);
    }

    private static GisFeature.Geometry readPolygon(ByteBuffer buffer) {
        buffer.position(buffer.position() + 32);
        int partCount = buffer.getInt();
        int pointCount = buffer.getInt();
        int[] partStarts = new int[partCount];
        for (int part = 0; part < partCount; part++) partStarts[part] = buffer.getInt();
        double[][] points = new double[pointCount][];
        for (int point = 0; point < pointCount; point++) points[point] = new double[]{buffer.getDouble(), buffer.getDouble()};
        List<GisFeature.Polygon> polygons = new ArrayList<>();
        for (int part = 0; part < partCount; part++) {
            int end = part + 1 < partCount ? partStarts[part + 1] : pointCount;
            List<double[]> ring = new ArrayList<>();
            for (int point = partStarts[part]; point < end; point++) ring.add(points[point]);
            // Shapefile: obrys zgodnie z ruchem wskazowek zegara, dziury przeciwnie.
            if (signedArea(ring) <= 0 || polygons.isEmpty()) {
                List<List<double[]>> rings = new ArrayList<>();
                rings.add(ring);
                polygons.add(new GisFeature.Polygon(rings));
            } else {
                polygons.getLast().rings().add(ring);
            }
        }
        return polygons.size() == 1 ? polygons.getFirst() : new GisFeature.MultiPolygon(polygons);
    }

    private static double signedArea(List<double[]> ring) {
        double sum = 0;
        for (int i = 0; i < ring.size() - 1; i++) sum += ring.get(i)[0] * ring.get(i + 1)[1] - ring.get(i + 1)[0] * ring.get(i)[1];
        return sum / 2;
    }

    private static String shapeTypeName(int shapeType) {
        return switch (shapeType) {
            case 3, 13, 23 -> "LineString";
            case 8, 18, 28 -> "MultiPoint";
            case 31 -> "MultiPatch";
            default -> "type " + shapeType;
        };
    }

    // ---------- .dbf ----------

    private static Charset charsetOf(byte[] cpg) {
        if (cpg == null) return null;
        String name = new String(cpg, StandardCharsets.US_ASCII).trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (name.contains("UTF")) return StandardCharsets.UTF_8;
        if (name.contains("1250")) return Charset.forName("windows-1250");
        if (name.contains("8859-2") || name.contains("88592")) return Charset.forName("ISO-8859-2");
        return null;
    }

    private static List<Map<String, String>> readDbf(byte[] dbf, Charset declaredCharset) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(dbf).order(ByteOrder.LITTLE_ENDIAN);
            int recordCount = buffer.getInt(4);
            int headerLength = Short.toUnsignedInt(buffer.getShort(8));
            int recordLength = Short.toUnsignedInt(buffer.getShort(10));
            List<String> names = new ArrayList<>();
            List<Integer> lengths = new ArrayList<>();
            for (int offset = 32; offset + 32 <= headerLength && dbf[offset] != 0x0D; offset += 32) {
                int nameEnd = offset;
                while (nameEnd < offset + 11 && dbf[nameEnd] != 0) nameEnd++;
                names.add(new String(dbf, offset, nameEnd - offset, StandardCharsets.ISO_8859_1).trim());
                lengths.add(Byte.toUnsignedInt(dbf[offset + 16]));
            }
            Charset charset = declaredCharset != null ? declaredCharset
                    : isValidUtf8(dbf, headerLength) ? StandardCharsets.UTF_8 : Charset.forName("windows-1250");
            List<Map<String, String>> records = new ArrayList<>();
            for (int record = 0; record < recordCount; record++) {
                int offset = headerLength + record * recordLength;
                if (offset + recordLength > dbf.length) break;
                Map<String, String> values = new LinkedHashMap<>();
                int fieldOffset = offset + 1;
                for (int field = 0; field < names.size(); field++) {
                    String value = new String(dbf, fieldOffset, lengths.get(field), charset).trim();
                    values.put(names.get(field), value.isEmpty() ? null : value);
                    fieldOffset += lengths.get(field);
                }
                records.add(values);
            }
            return records;
        } catch (IndexOutOfBoundsException corrupted) {
            throw new GisFileException(GisFileException.Reason.UNREADABLE_FILE, "dbf");
        }
    }

    private static boolean isValidUtf8(byte[] bytes, int from) {
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, from, bytes.length - from));
            return true;
        } catch (CharacterCodingException notUtf8) {
            return false;
        }
    }
}
