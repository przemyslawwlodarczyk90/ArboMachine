package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Etap 34: style warstw eksportu dla QGIS (pliki QML) - zeby po otwarciu GeoPackage drzewa nie byly jednakowymi
 * kropkami. trees: zielony punkt z numerem drzewa; tree_markings: kategorie wg kodu oznaczenia w kolorach i skrotach
 * z katalogu, kazde oznaczenie na stalym miejscu wokol punktu drzewa (16 pozycji na okregu), wiec kilka oznaczen
 * jednego drzewa sie nie zaslania; stands: polprzezroczysty obszar z nazwa.
 * Format QML QGIS 3 (zapis "Option"), styl V1 - NIE sprawdzony w samym QGIS (patrz !!! KONSPEKTY NOWE, etap 34).
 */
@Component
public class QgisLayerStyles {

    /** Wersja QGIS wpisana w naglowek QML - QGIS 3.44 i 4.x czytaja ten format. */
    static final String QGIS_VERSION = "3.44.0";
    /** Odleglosc znaczka oznaczenia od punktu drzewa (mm na wydruku / ekranie). */
    private static final double MARKING_RING_RADIUS_MM = 5.0;
    private static final double MARKING_SYMBOL_SIZE_MM = 3.6;

    private final TreeMarkingCatalog treeMarkingCatalog;

    public QgisLayerStyles(TreeMarkingCatalog treeMarkingCatalog) {
        this.treeMarkingCatalog = treeMarkingCatalog;
    }

    public String treesStyle() {
        return document("Symbology|Labeling", """
                  <renderer-v2 type="singleSymbol" symbollevels="0" enableorderby="0" forceraster="0">
                    <symbols>
                %s
                    </symbols>
                  </renderer-v2>
                %s""".formatted(circleSymbol("0", "47,107,60,255", 3.0, "0,0", null), label("inventory_no", "2")));
    }

    public String treeMarkingsStyle() {
        List<TreeMarkingDefinition> markings = treeMarkingCatalog.allMarkings();
        StringBuilder categories = new StringBuilder();
        StringBuilder symbols = new StringBuilder();
        for (int index = 0; index < markings.size(); index++) {
            TreeMarkingDefinition marking = markings.get(index);
            categories.append("      <category value=\"%s\" symbol=\"%d\" label=\"%s\" render=\"true\"/>\n"
                    .formatted(xml(marking.code()), index, xml(marking.displayName())));
            symbols.append(circleSymbol(String.valueOf(index), rgba(marking.color()), MARKING_SYMBOL_SIZE_MM,
                    ringOffset(index, markings.size()), marking.shortLabel())).append('\n');
        }
        return document("Symbology", """
                  <renderer-v2 type="categorizedSymbol" attr="marking_code" symbollevels="0" enableorderby="0" forceraster="0">
                    <categories>
                %s    </categories>
                    <symbols>
                %s    </symbols>
                  </renderer-v2>
                """.formatted(categories, symbols));
    }

    public String standsStyle() {
        return document("Symbology|Labeling", """
                  <renderer-v2 type="singleSymbol" symbollevels="0" enableorderby="0" forceraster="0">
                    <symbols>
                      <symbol type="fill" name="0" alpha="1" clip_to_extent="1" force_rhr="0">
                        <layer class="SimpleFill" enabled="1" locked="0" pass="0">
                          <Option type="Map">
                            <Option name="color" value="64,160,43,46" type="QString"/>
                            <Option name="outline_color" value="47,158,68,255" type="QString"/>
                            <Option name="outline_style" value="solid" type="QString"/>
                            <Option name="outline_width" value="0.6" type="QString"/>
                            <Option name="outline_width_unit" value="MM" type="QString"/>
                            <Option name="style" value="solid" type="QString"/>
                          </Option>
                        </layer>
                      </symbol>
                    </symbols>
                  </renderer-v2>
                %s""".formatted(label("name", "1")));
    }

    /** Pozycja oznaczenia na okregu wokol drzewa: pierwsze oznaczenie katalogu nad punktem, dalej zgodnie z zegarem. */
    static String ringOffset(int index, int count) {
        double angle = 2 * Math.PI * index / count;
        double x = MARKING_RING_RADIUS_MM * Math.sin(angle);
        double y = -MARKING_RING_RADIUS_MM * Math.cos(angle);
        return String.format(Locale.ROOT, "%.2f,%.2f", x, y);
    }

    private static String circleSymbol(String name, String fillRgba, double sizeMm, String offset, String characterOrNull) {
        String fontLayer = characterOrNull == null ? "" : """
                        <layer class="FontMarker" enabled="1" locked="0" pass="0">
                          <Option type="Map">
                            <Option name="chr" value="%s" type="QString"/>
                            <Option name="font" value="Arial" type="QString"/>
                            <Option name="font_style" value="Bold" type="QString"/>
                            <Option name="color" value="255,255,255,255" type="QString"/>
                            <Option name="size" value="%s" type="QString"/>
                            <Option name="size_unit" value="MM" type="QString"/>
                            <Option name="offset" value="%s" type="QString"/>
                            <Option name="offset_unit" value="MM" type="QString"/>
                          </Option>
                        </layer>
                """.formatted(xml(characterOrNull), number(sizeMm * (characterOrNull.length() > 1 ? 0.5 : 0.65)), offset);
        return """
                      <symbol type="marker" name="%s" alpha="1" clip_to_extent="1" force_rhr="0">
                        <layer class="SimpleMarker" enabled="1" locked="0" pass="0">
                          <Option type="Map">
                            <Option name="name" value="circle" type="QString"/>
                            <Option name="color" value="%s" type="QString"/>
                            <Option name="outline_color" value="255,255,255,255" type="QString"/>
                            <Option name="outline_style" value="solid" type="QString"/>
                            <Option name="outline_width" value="0.4" type="QString"/>
                            <Option name="outline_width_unit" value="MM" type="QString"/>
                            <Option name="size" value="%s" type="QString"/>
                            <Option name="size_unit" value="MM" type="QString"/>
                            <Option name="offset" value="%s" type="QString"/>
                            <Option name="offset_unit" value="MM" type="QString"/>
                          </Option>
                        </layer>
                %s          </symbol>""".formatted(name, fillRgba, number(sizeMm), offset, fontLayer);
    }

    private static String label(String fieldName, String distanceMm) {
        return """
                  <labeling type="simple">
                    <settings calloutType="simple">
                      <text-style fieldName="%s" isExpression="0" fontFamily="Arial" fontSize="8" fontSizeUnit="Point" textColor="29,36,25,255">
                        <text-buffer bufferDraw="1" bufferSize="0.8" bufferSizeUnits="MM" bufferColor="255,255,255,255"/>
                      </text-style>
                      <placement placement="1" quadOffset="5" dist="%s" distUnits="MM"/>
                      <rendering scaleVisibility="0" obstacle="1"/>
                    </settings>
                  </labeling>
                """.formatted(fieldName, distanceMm);
    }

    private static String document(String styleCategories, String body) {
        boolean hasLabels = body.contains("<labeling");
        return """
                <!DOCTYPE qgis PUBLIC 'http://mrcc.com/qgis.dtd' 'SYSTEM'>
                <qgis version="%s" styleCategories="%s" labelsEnabled="%d">
                %s</qgis>
                """.formatted(QGIS_VERSION, styleCategories, hasLabels ? 1 : 0, body);
    }

    /** "#2E7D32" -> "46,125,50,255" (format kolorow QGIS). */
    static String rgba(String hexColor) {
        int rgb = Integer.parseInt(hexColor.substring(1), 16);
        return "%d,%d,%d,255".formatted((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String xml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
