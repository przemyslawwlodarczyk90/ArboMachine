package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.gis.DxfDrawingReader;
import com.example.arbomachine.domain.dataexchange.gis.DxfLayerExtractor;
import com.example.arbomachine.domain.dataexchange.gis.GisFileException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Rysunek DXF drzew - etap 29: punkty drzew z warstw "-Point" z numerami z opisow (DxfLayerExtractor). Z rysunku
 * bierzemy tylko numer i polozenie - obwod i korona narysowane jako okregi NIE sa importowane (atrybuty zostaja
 * w arkuszu; DO_DOPRECYZOWANIA).
 */
@Component
public class DxfTreeImportParser implements TreeImportParser {

    @Override
    public TreeImportFormat format() {
        return TreeImportFormat.DXF;
    }

    @Override
    public ParsedTreeImportFile parse(byte[] content) {
        try {
            DxfDrawingReader.DxfDrawing drawing = DxfDrawingReader.read(content);
            // Warstwa obszarow tylko po to, by rysunek bez drzew zglosil "brak punktow" (do importu grup drzew).
            return GisTreeImportFiles.withConversionWarning(GisTreeRows.fromLayers(
                    List.of(DxfLayerExtractor.treeLayer(drawing), DxfLayerExtractor.standLayer(drawing))))
                    .withFileWarning(TreeImportIssue.ofFile(TreeImportIssueCode.NUMBERS_READ_FROM_DRAWING_LABELS, null));
        } catch (GisFileException gisFileException) {
            throw GisTreeRows.fileExceptionOf(gisFileException);
        }
    }
}
