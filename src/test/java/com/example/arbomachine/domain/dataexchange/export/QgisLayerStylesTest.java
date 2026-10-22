package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Etap 34: style QML - poprawny XML, kategorie wszystkich oznaczen katalogu, rozne pozycje znaczkow. */
class QgisLayerStylesTest {

    private final TreeMarkingCatalog catalog = new TreeMarkingCatalog();
    private final QgisLayerStyles styles = new QgisLayerStyles(catalog);

    private static Document parse(String qml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // DOCTYPE QGIS wskazuje zewnetrzny DTD - nie pobieramy go.
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(qml.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void allStylesAreWellFormedQgisDocuments() throws Exception {
        for (String qml : new String[]{styles.treesStyle(), styles.treeMarkingsStyle(), styles.standsStyle()}) {
            Element root = parse(qml).getDocumentElement();
            assertThat(root.getTagName()).isEqualTo("qgis");
            assertThat(root.getAttribute("version")).isEqualTo(QgisLayerStyles.QGIS_VERSION);
            assertThat(root.getElementsByTagName("renderer-v2").getLength()).isEqualTo(1);
        }
        assertThat(parse(styles.treesStyle()).getDocumentElement().getAttribute("labelsEnabled")).isEqualTo("1");
    }

    @Test
    void markingsStyleHasOneCategoryPerCatalogCodeWithCatalogColorAndDistinctOffsets() throws Exception {
        Document document = parse(styles.treeMarkingsStyle());
        Element renderer = (Element) document.getElementsByTagName("renderer-v2").item(0);
        assertThat(renderer.getAttribute("type")).isEqualTo("categorizedSymbol");
        assertThat(renderer.getAttribute("attr")).isEqualTo("marking_code");
        NodeList categories = document.getElementsByTagName("category");
        Set<String> codes = new HashSet<>();
        for (int index = 0; index < categories.getLength(); index++) codes.add(((Element) categories.item(index)).getAttribute("value"));
        assertThat(codes).hasSize(catalog.allMarkings().size()).contains("TO_REMOVE", "VALUABLE", "SNAG");
        assertThat(styles.treeMarkingsStyle()).contains("198,40,40,255"); // TO_REMOVE #C62828
        assertThat(styles.treeMarkingsStyle()).contains("Drzewo wartościowe");
        Set<String> offsets = new HashSet<>();
        for (int index = 0; index < 16; index++) offsets.add(QgisLayerStyles.ringOffset(index, 16));
        assertThat(offsets).hasSize(16);
        assertThat(QgisLayerStyles.ringOffset(0, 16)).isEqualTo("0.00,-5.00");
    }

    @Test
    void hexColorBecomesQgisRgba() {
        assertThat(QgisLayerStyles.rgba("#2E7D32")).isEqualTo("46,125,50,255");
    }
}
