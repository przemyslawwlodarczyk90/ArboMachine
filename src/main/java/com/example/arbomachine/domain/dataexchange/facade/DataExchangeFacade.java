package com.example.arbomachine.domain.dataexchange.facade;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportResultResponse;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.dto.TreeStandImportResultResponse;
import com.example.arbomachine.domain.dataexchange.dto.TreeStandImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreeWorksAsCsvHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportProjectAsShapefileHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreeStandsAsGeoJsonHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ImportTreeStandsHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ValidateTreeStandImportHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreeWorksAsIcsHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreesAsCsvHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreesAsGeoJsonHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportQgisPackageHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreesAsGeoPackageHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreesAsXlsxHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ImportTreesHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ValidateTreeImportHandler;
import org.springframework.stereotype.Component;

/**
 * Import i eksport danych projektu (etapy 23-24). Formaty zewnetrzne to tylko adaptery wejscia/wyjscia - domena
 * Project -> Tree nie zalezy od zadnego z nich. Tylko deleguje do handlerow.
 */
@Component
public class DataExchangeFacade {
    private final ExportTreesAsCsvHandler exportTreesAsCsvHandler;
    private final ExportTreesAsXlsxHandler exportTreesAsXlsxHandler;
    private final ExportTreesAsGeoJsonHandler exportTreesAsGeoJsonHandler;
    private final ExportTreesAsGeoPackageHandler exportTreesAsGeoPackageHandler;
    private final ExportQgisPackageHandler exportQgisPackageHandler;
    private final ValidateTreeImportHandler validateTreeImportHandler;
    private final ImportTreesHandler importTreesHandler;
    private final ExportTreeWorksAsCsvHandler exportTreeWorksAsCsvHandler;
    private final ExportTreeWorksAsIcsHandler exportTreeWorksAsIcsHandler;
    private final ValidateTreeStandImportHandler validateTreeStandImportHandler;
    private final ImportTreeStandsHandler importTreeStandsHandler;
    private final ExportTreeStandsAsGeoJsonHandler exportTreeStandsAsGeoJsonHandler;
    private final ExportProjectAsShapefileHandler exportProjectAsShapefileHandler;

    public DataExchangeFacade(ExportTreesAsCsvHandler exportTreesAsCsvHandler, ExportTreesAsXlsxHandler exportTreesAsXlsxHandler,
                              ExportTreesAsGeoJsonHandler exportTreesAsGeoJsonHandler,
                              ExportTreesAsGeoPackageHandler exportTreesAsGeoPackageHandler,
                              ExportQgisPackageHandler exportQgisPackageHandler,
                              ValidateTreeImportHandler validateTreeImportHandler, ImportTreesHandler importTreesHandler,
                              ExportTreeWorksAsCsvHandler exportTreeWorksAsCsvHandler,
                              ExportTreeWorksAsIcsHandler exportTreeWorksAsIcsHandler,
                              ValidateTreeStandImportHandler validateTreeStandImportHandler,
                              ImportTreeStandsHandler importTreeStandsHandler,
                              ExportTreeStandsAsGeoJsonHandler exportTreeStandsAsGeoJsonHandler,
                              ExportProjectAsShapefileHandler exportProjectAsShapefileHandler) {
        this.exportTreesAsCsvHandler = exportTreesAsCsvHandler;
        this.exportTreesAsXlsxHandler = exportTreesAsXlsxHandler;
        this.exportTreesAsGeoJsonHandler = exportTreesAsGeoJsonHandler;
        this.exportTreesAsGeoPackageHandler = exportTreesAsGeoPackageHandler;
        this.exportQgisPackageHandler = exportQgisPackageHandler;
        this.validateTreeImportHandler = validateTreeImportHandler;
        this.importTreesHandler = importTreesHandler;
        this.exportTreeWorksAsCsvHandler = exportTreeWorksAsCsvHandler;
        this.exportTreeWorksAsIcsHandler = exportTreeWorksAsIcsHandler;
        this.validateTreeStandImportHandler = validateTreeStandImportHandler;
        this.importTreeStandsHandler = importTreeStandsHandler;
        this.exportTreeStandsAsGeoJsonHandler = exportTreeStandsAsGeoJsonHandler;
        this.exportProjectAsShapefileHandler = exportProjectAsShapefileHandler;
    }

    public ExportedFile exportTreesAsCsvOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreesAsCsvHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportTreesAsXlsxOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreesAsXlsxHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportTreesAsGeoJsonOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreesAsGeoJsonHandler.handle(currentUserEmail, projectId);
    }

    /** requestedCrs: null = EPSG:4326; EPSG:2180, EPSG:2176-2179 albo "PL-2000" (strefa wg drzew projektu). */
    public ExportedFile exportTreesAsGeoPackageOfCurrentUser(String currentUserEmail, Long projectId, String requestedCrs) {
        return exportTreesAsGeoPackageHandler.handle(currentUserEmail, projectId, requestedCrs);
    }

    /** Etap 34: pakiet dla QGIS (ZIP) w wybranym ukladzie. */
    public ExportedFile exportQgisPackageOfCurrentUser(String currentUserEmail, Long projectId, String requestedCrs) {
        return exportQgisPackageHandler.handle(currentUserEmail, projectId, requestedCrs);
    }

    public ExportedFile exportTreeWorksAsCsvOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreeWorksAsCsvHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportTreeWorksAsIcsOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreeWorksAsIcsHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportTreeStandsAsGeoJsonOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreeStandsAsGeoJsonHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportProjectAsShapefileOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportProjectAsShapefileHandler.handle(currentUserEmail, projectId);
    }

    public TreeStandImportValidationResponse validateTreeStandImportOfCurrentUser(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        return validateTreeStandImportHandler.handle(currentUserEmail, projectId, upload);
    }

    public TreeStandImportResultResponse importTreeStandsOfCurrentUser(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        return importTreeStandsHandler.handle(currentUserEmail, projectId, upload);
    }

    public TreeImportValidationResponse validateTreeImportOfCurrentUser(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        return validateTreeImportHandler.handle(currentUserEmail, projectId, upload);
    }

    public TreeImportResultResponse importTreesOfCurrentUser(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        return importTreesHandler.handle(currentUserEmail, projectId, upload);
    }
}
