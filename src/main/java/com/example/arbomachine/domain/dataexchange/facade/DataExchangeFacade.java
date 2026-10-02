package com.example.arbomachine.domain.dataexchange.facade;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportResultResponse;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.export.ExportedFile;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreeWorksAsCsvHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreeWorksAsIcsHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreesAsCsvHandler;
import com.example.arbomachine.domain.dataexchange.service.dataexchangefacade.ExportTreesAsGeoJsonHandler;
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
    private final ValidateTreeImportHandler validateTreeImportHandler;
    private final ImportTreesHandler importTreesHandler;
    private final ExportTreeWorksAsCsvHandler exportTreeWorksAsCsvHandler;
    private final ExportTreeWorksAsIcsHandler exportTreeWorksAsIcsHandler;

    public DataExchangeFacade(ExportTreesAsCsvHandler exportTreesAsCsvHandler, ExportTreesAsXlsxHandler exportTreesAsXlsxHandler,
                              ExportTreesAsGeoJsonHandler exportTreesAsGeoJsonHandler,
                              ExportTreesAsGeoPackageHandler exportTreesAsGeoPackageHandler,
                              ValidateTreeImportHandler validateTreeImportHandler, ImportTreesHandler importTreesHandler,
                              ExportTreeWorksAsCsvHandler exportTreeWorksAsCsvHandler,
                              ExportTreeWorksAsIcsHandler exportTreeWorksAsIcsHandler) {
        this.exportTreesAsCsvHandler = exportTreesAsCsvHandler;
        this.exportTreesAsXlsxHandler = exportTreesAsXlsxHandler;
        this.exportTreesAsGeoJsonHandler = exportTreesAsGeoJsonHandler;
        this.exportTreesAsGeoPackageHandler = exportTreesAsGeoPackageHandler;
        this.validateTreeImportHandler = validateTreeImportHandler;
        this.importTreesHandler = importTreesHandler;
        this.exportTreeWorksAsCsvHandler = exportTreeWorksAsCsvHandler;
        this.exportTreeWorksAsIcsHandler = exportTreeWorksAsIcsHandler;
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

    public ExportedFile exportTreesAsGeoPackageOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreesAsGeoPackageHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportTreeWorksAsCsvOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreeWorksAsCsvHandler.handle(currentUserEmail, projectId);
    }

    public ExportedFile exportTreeWorksAsIcsOfCurrentUser(String currentUserEmail, Long projectId) {
        return exportTreeWorksAsIcsHandler.handle(currentUserEmail, projectId);
    }

    public TreeImportValidationResponse validateTreeImportOfCurrentUser(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        return validateTreeImportHandler.handle(currentUserEmail, projectId, upload);
    }

    public TreeImportResultResponse importTreesOfCurrentUser(String currentUserEmail, Long projectId, TreeImportUpload upload) {
        return importTreesHandler.handle(currentUserEmail, projectId, upload);
    }
}
