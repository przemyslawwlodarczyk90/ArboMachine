package com.example.arbomachine.domain.dataexchange.imports;

/** Wspolne dla parserow GIS: ostrzezenie o przeliczeniu ukladu wspolrzednych (kod EPSG zrodla w kolumnie). */
final class GisTreeImportFiles {

    private GisTreeImportFiles() {
    }

    static ParsedTreeImportFile withConversionWarning(GisTreeRows.ConvertedTreeFile convertedFile) {
        if (convertedFile.sourceCrs() == null) return convertedFile.file();
        return convertedFile.file().withFileWarning(TreeImportIssue.ofFile(TreeImportIssueCode.COORDINATES_CONVERTED,
                convertedFile.sourceCrs().code()));
    }
}
