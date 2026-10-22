package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.crs.CoordinateTransformer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Etap 34: pakiet dla QGIS (ZIP) - GeoPackage z trzema warstwami i stylami w layer_styles, te same style jako
 * osobne pliki .qml (do recznego wczytania: Wlasciwosci warstwy -> Styl -> Wczytaj styl) i README z instrukcja.
 * V1 bez projektu .qgz - QGIS sam doda warstwy z GeoPackage przeciagnietego do okna.
 */
@Component
public class QgisPackageExporter {

    public static final String CONTENT_TYPE = "application/zip";

    private final TreeGeoPackageExporter treeGeoPackageExporter;
    private final QgisLayerStyles qgisLayerStyles;

    public QgisPackageExporter(TreeGeoPackageExporter treeGeoPackageExporter, QgisLayerStyles qgisLayerStyles) {
        this.treeGeoPackageExporter = treeGeoPackageExporter;
        this.qgisLayerStyles = qgisLayerStyles;
    }

    public ExportedFile export(ProjectExportData data, CoordinateTransformer coordinateTransformer) {
        ExportedFile geoPackage = treeGeoPackageExporter.export(data, coordinateTransformer);
        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes, StandardCharsets.UTF_8)) {
            addEntry(zip, geoPackage.fileName(), geoPackage.content());
            addEntry(zip, TreeGeoPackageExporter.LAYER_NAME + ".qml", utf8(qgisLayerStyles.treesStyle()));
            addEntry(zip, TreeGeoPackageExporter.MARKINGS_LAYER_NAME + ".qml", utf8(qgisLayerStyles.treeMarkingsStyle()));
            addEntry(zip, TreeGeoPackageExporter.STANDS_LAYER_NAME + ".qml", utf8(qgisLayerStyles.standsStyle()));
            addEntry(zip, "README.txt", utf8(readme(data, geoPackage.fileName(), coordinateTransformer.targetCrsCode())));
        } catch (IOException writingError) {
            throw new UncheckedIOException(writingError);
        }
        return new ExportedFile(zipBytes.toByteArray(), CONTENT_TYPE, "arbomachine-projekt-" + data.projectId() + "-qgis.zip");
    }

    private static String readme(ProjectExportData data, String geoPackageFileName, String crsCode) {
        return """
                ArboMachine - pakiet dla QGIS
                Projekt: %d, eksport: %s, uklad wspolrzednych: %s

                Zawartosc:
                - %s - GeoPackage z warstwami:
                    trees          drzewa z lokalizacja (punkty, atrybuty drzewa)
                    tree_markings  oznaczenia drzew - jedno oznaczenie = jeden punkt w miejscu drzewa
                                   (marking_code = stabilny kod, marking_label = nazwa)
                    stands         grupy drzew (obszary)
                - trees.qml, tree_markings.qml, stands.qml - style warstw (te same, ktore sa zapisane w GeoPackage)

                Jak otworzyc:
                1. Rozpakuj archiwum.
                2. Przeciagnij plik .gpkg do okna QGIS (albo Warstwa -> Dodaj warstwe -> Wektorowa) i wybierz wszystkie warstwy.
                3. Style wczytuja sie same. Jesli nie: Wlasciwosci warstwy -> Styl -> Wczytaj styl -> plik .qml o nazwie warstwy.

                Drzewa bez lokalizacji nie sa w pliku. Styl to wersja V1 - nie byl jeszcze sprawdzony w kazdej wersji QGIS.
                Kolejnosc wspolrzednych: x = wschod (easting / dlugosc), y = polnoc (northing / szerokosc).
                """.formatted(data.projectId(), data.exportedAt(), crsCode, geoPackageFileName).replace("\n", "\r\n");
    }

    private static void addEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
