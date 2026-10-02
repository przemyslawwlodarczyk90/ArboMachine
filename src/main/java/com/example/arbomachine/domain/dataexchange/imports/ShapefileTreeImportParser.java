package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.gis.GisFileException;
import com.example.arbomachine.domain.dataexchange.gis.ShapefileZipReader;
import org.springframework.stereotype.Component;

/** Shapefile (ZIP) drzew - etap 29: warstwy punktowe, atrybuty DBF jak naglowki CSV, uklad z .prj albo odgadniety. */
@Component
public class ShapefileTreeImportParser implements TreeImportParser {

    @Override
    public TreeImportFormat format() {
        return TreeImportFormat.SHAPEFILE;
    }

    @Override
    public ParsedTreeImportFile parse(byte[] content) {
        try {
            return GisTreeImportFiles.withConversionWarning(GisTreeRows.fromLayers(ShapefileZipReader.read(content)));
        } catch (GisFileException gisFileException) {
            throw GisTreeRows.fileExceptionOf(gisFileException);
        }
    }
}
