package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.gis.GeoPackageFileReader;
import com.example.arbomachine.domain.dataexchange.gis.GisFileException;
import org.springframework.stereotype.Component;

/** GeoPackage drzew - etap 29: tabele punktowe (np. eksport ArboMachine po edycji w QGIS), uklad z SRS warstwy. */
@Component
public class GeoPackageTreeImportParser implements TreeImportParser {

    @Override
    public TreeImportFormat format() {
        return TreeImportFormat.GEOPACKAGE;
    }

    @Override
    public ParsedTreeImportFile parse(byte[] content) {
        try {
            return GisTreeImportFiles.withConversionWarning(GisTreeRows.fromLayers(GeoPackageFileReader.read(content)));
        } catch (GisFileException gisFileException) {
            throw GisTreeRows.fileExceptionOf(gisFileException);
        }
    }
}
