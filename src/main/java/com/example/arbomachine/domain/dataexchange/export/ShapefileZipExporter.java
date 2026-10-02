package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Shapefile (ZIP) projektu - etap 29: warstwa "trees" (Point, atrybuty TreeExchangeField.GIS_ATTRIBUTES pod nazwami DBF
 * do 10 znakow) i "stands" (Polygon, grupy drzew). WGS 84 (.prj), atrybuty UTF-8 (.cpg). Tekst w DBF ma najwyzej
 * 254 bajty - dluzsze opisy sa skracane (pelne: GeoPackage / XLSX). Drzewa bez lokalizacji pominiete. Bez bibliotek.
 */
@Component
public class ShapefileZipExporter {

    static final String WGS84_PRJ = "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\",SPHEROID[\"WGS_1984\",6378137.0,298.257223563]],"
            + "PRIMEM[\"Greenwich\",0.0],UNIT[\"Degree\",0.0174532925199433]]";
    private static final int SHAPE_POINT = 1;
    private static final int SHAPE_POLYGON = 5;
    private static final int MAXIMUM_TEXT_BYTES = 254;

    public ExportedFile export(ProjectExportData data) {
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes, StandardCharsets.UTF_8)) {
            List<TreeExportData> locatedTrees = data.trees().stream().filter(TreeExportData::hasLocation).toList();
            List<DbfColumn<TreeExportData>> treeColumns = TreeExchangeField.GIS_ATTRIBUTES.stream()
                    .map(field -> new DbfColumn<TreeExportData>(field.shapefileFieldName(), field.valueType() == TreeExchangeField.ValueType.TEXT
                            ? 'C' : 'N', field.valueType() == TreeExchangeField.ValueType.DECIMAL ? 2 : 0, tree -> tree.valueOf(field)))
                    .toList();
            writeLayer(zip, "trees", SHAPE_POINT, locatedTrees.stream().map(tree -> List.of(List.of(
                    new double[]{tree.longitude().doubleValue(), tree.latitude().doubleValue()}))).toList(), locatedTrees, treeColumns);
            List<DbfColumn<TreeStandExportData>> standColumns = List.of(
                    new DbfColumn<>("stand_id", 'N', 0, TreeStandExportData::standId),
                    new DbfColumn<>("name", 'C', 0, TreeStandExportData::name),
                    new DbfColumn<>("area_m2", 'N', 2, TreeStandExportData::areaSquareMeters),
                    new DbfColumn<>("tree_count", 'N', 0, stand -> stand.treeInventoryNumbers().size()),
                    new DbfColumn<>("tree_nos", 'C', 0, TreeStandExportData::treeNumbersText),
                    new DbfColumn<>("notes", 'C', 0, TreeStandExportData::notes),
                    new DbfColumn<>("source", 'C', 0, TreeStandExportData::source));
            writeLayer(zip, "stands", SHAPE_POLYGON, data.stands().stream().map(TreeStandExportData::rings).toList(), data.stands(), standColumns);
        } catch (IOException writingError) {
            throw new UncheckedIOException(writingError);
        }
        return new ExportedFile(zipBytes.toByteArray(), "application/zip", "arbomachine-projekt-" + data.projectId() + "-shapefile.zip");
    }

    record DbfColumn<T>(String name, char type, int decimals, Function<T, Object> value) {}

    private static <T> void writeLayer(ZipOutputStream zip, String layerName, int shapeType, List<List<List<double[]>>> geometries,
                                       List<T> records, List<DbfColumn<T>> columns) throws IOException {
        List<byte[]> contents = geometries.stream().map(rings -> shapeType == SHAPE_POINT ? pointContent(rings.getFirst().getFirst())
                : polygonContent(rings)).toList();
        double[] bounds = boundsOf(geometries);
        ByteArrayOutputStream shp = new ByteArrayOutputStream();
        ByteArrayOutputStream shx = new ByteArrayOutputStream();
        int shpLengthWords = 50 + contents.stream().mapToInt(content -> 4 + content.length / 2).sum();
        shp.writeBytes(header(shapeType, shpLengthWords, bounds));
        shx.writeBytes(header(shapeType, 50 + 4 * contents.size(), bounds));
        int offsetWords = 50;
        for (int index = 0; index < contents.size(); index++) {
            byte[] content = contents.get(index);
            shp.writeBytes(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putInt(index + 1).putInt(content.length / 2).array());
            shp.writeBytes(content);
            shx.writeBytes(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putInt(offsetWords).putInt(content.length / 2).array());
            offsetWords += 4 + content.length / 2;
        }
        add(zip, layerName + ".shp", shp.toByteArray());
        add(zip, layerName + ".shx", shx.toByteArray());
        add(zip, layerName + ".dbf", dbf(records, columns));
        add(zip, layerName + ".prj", WGS84_PRJ.getBytes(StandardCharsets.US_ASCII));
        add(zip, layerName + ".cpg", "UTF-8".getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] header(int shapeType, int fileLengthWords, double[] bounds) {
        ByteBuffer header = ByteBuffer.allocate(100);
        header.order(ByteOrder.BIG_ENDIAN).putInt(0, 9994).putInt(24, fileLengthWords);
        header.order(ByteOrder.LITTLE_ENDIAN).putInt(28, 1000).putInt(32, shapeType);
        header.putDouble(36, bounds[0]).putDouble(44, bounds[1]).putDouble(52, bounds[2]).putDouble(60, bounds[3]);
        return header.array();
    }

    private static byte[] pointContent(double[] position) {
        return ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN).putInt(SHAPE_POINT).putDouble(position[0]).putDouble(position[1]).array();
    }

    /** Shapefile: obrys zgodnie z ruchem wskazowek zegara, dziury przeciwnie (odwrotnie niz GeoJSON RFC 7946). */
    private static byte[] polygonContent(List<List<double[]>> rings) {
        List<List<double[]>> oriented = new ArrayList<>();
        for (int index = 0; index < rings.size(); index++) {
            List<double[]> ring = new ArrayList<>(rings.get(index));
            boolean clockwise = signedArea(ring) < 0;
            if ((index == 0) != clockwise) java.util.Collections.reverse(ring);
            oriented.add(ring);
        }
        int pointCount = oriented.stream().mapToInt(List::size).sum();
        double[] bounds = boundsOf(List.of(rings));
        ByteBuffer content = ByteBuffer.allocate(44 + 4 * oriented.size() + 16 * pointCount).order(ByteOrder.LITTLE_ENDIAN);
        content.putInt(SHAPE_POLYGON).putDouble(bounds[0]).putDouble(bounds[1]).putDouble(bounds[2]).putDouble(bounds[3]);
        content.putInt(oriented.size()).putInt(pointCount);
        int start = 0;
        for (List<double[]> ring : oriented) {
            content.putInt(start);
            start += ring.size();
        }
        for (List<double[]> ring : oriented) for (double[] position : ring) content.putDouble(position[0]).putDouble(position[1]);
        return content.array();
    }

    private static double signedArea(List<double[]> ring) {
        double sum = 0;
        for (int i = 0; i < ring.size() - 1; i++) sum += ring.get(i)[0] * ring.get(i + 1)[1] - ring.get(i + 1)[0] * ring.get(i)[1];
        return sum / 2;
    }

    private static double[] boundsOf(List<List<List<double[]>>> geometries) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (List<List<double[]>> rings : geometries) for (List<double[]> ring : rings) for (double[] position : ring) {
            minX = Math.min(minX, position[0]);
            minY = Math.min(minY, position[1]);
            maxX = Math.max(maxX, position[0]);
            maxY = Math.max(maxY, position[1]);
        }
        return geometries.isEmpty() ? new double[]{0, 0, 0, 0} : new double[]{minX, minY, maxX, maxY};
    }

    // ---------- DBF (dBASE III) ----------

    private static <T> byte[] dbf(List<T> records, List<DbfColumn<T>> columns) {
        List<List<byte[]>> values = new ArrayList<>();
        int[] widths = new int[columns.size()];
        for (T record : records) {
            List<byte[]> row = new ArrayList<>();
            for (int column = 0; column < columns.size(); column++) {
                byte[] value = formatted(columns.get(column), columns.get(column).value().apply(record));
                widths[column] = Math.max(widths[column], value.length);
                row.add(value);
            }
            values.add(row);
        }
        for (int column = 0; column < columns.size(); column++) {
            widths[column] = Math.max(1, Math.min(columns.get(column).type() == 'C' ? MAXIMUM_TEXT_BYTES : 20, Math.max(widths[column],
                    columns.get(column).type() == 'N' ? 10 : 1)));
        }
        int headerLength = 32 + 32 * columns.size() + 1;
        int recordLength = 1 + java.util.Arrays.stream(widths).sum();
        ByteBuffer dbf = ByteBuffer.allocate(headerLength + recordLength * records.size() + 1).order(ByteOrder.LITTLE_ENDIAN);
        dbf.put((byte) 0x03).put((byte) 126).put((byte) 10).put((byte) 2);
        dbf.putInt(records.size()).putShort((short) headerLength).putShort((short) recordLength);
        dbf.position(32);
        for (int column = 0; column < columns.size(); column++) {
            byte[] name = new byte[11];
            byte[] nameBytes = columns.get(column).name().getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(nameBytes, 0, name, 0, Math.min(10, nameBytes.length));
            dbf.put(name).put((byte) columns.get(column).type()).putInt(0)
                    .put((byte) widths[column]).put((byte) (columns.get(column).type() == 'N' ? columns.get(column).decimals() : 0));
            dbf.put(new byte[14]);
        }
        dbf.put((byte) 0x0D);
        for (List<byte[]> row : values) {
            dbf.put((byte) ' ');
            for (int column = 0; column < columns.size(); column++) {
                byte[] padded = new byte[widths[column]];
                java.util.Arrays.fill(padded, (byte) ' ');
                byte[] value = row.get(column);
                int length = Math.min(value.length, widths[column]);
                // liczby do prawej, tekst do lewej (jak w dBASE)
                System.arraycopy(value, 0, padded, columns.get(column).type() == 'N' ? widths[column] - length : 0, length);
                dbf.put(padded);
            }
        }
        dbf.put((byte) 0x1A);
        return dbf.array();
    }

    private static byte[] formatted(DbfColumn<?> column, Object value) {
        if (value == null) return new byte[0];
        if (column.type() == 'N') {
            BigDecimal number = value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
            return number.setScale(column.decimals(), RoundingMode.HALF_UP).toPlainString().getBytes(StandardCharsets.US_ASCII);
        }
        return truncatedUtf8(value.toString(), MAXIMUM_TEXT_BYTES);
    }

    /** Najwyzej maxBytes bajtow UTF-8, bez przecinania znaku. */
    private static byte[] truncatedUtf8(String text, int maxBytes) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) return bytes;
        int end = maxBytes;
        while (end > 0 && (bytes[end] & 0xC0) == 0x80) end--;
        return java.util.Arrays.copyOf(bytes, end);
    }

    private static void add(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}
