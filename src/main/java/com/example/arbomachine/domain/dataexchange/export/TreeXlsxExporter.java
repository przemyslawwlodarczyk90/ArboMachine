package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import com.example.arbomachine.domain.dataexchange.schema.MonitoringExchangeColumn;
import com.example.arbomachine.domain.dataexchange.schema.WorkExchangeColumn;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * XLSX drzew V1 (fastexcel): arkusz "Drzewa" - naglowki po polsku (pogrubione, zamrozony pierwszy wiersz),
 * liczby jako liczby (nie tekst), puste komorki dla brakow; arkusz "Prace" (etap 28) - jeden wiersz na prace;
 * arkusz "Monitoring" (etap 29) - jeden wiersz na kontrole stanu;
 * arkusz "Projekt" - podstawowe informacje i wersja schematu.
 * Bez dalszego stylowania.
 */
@Component
public class TreeXlsxExporter {

    public static final String TREES_SHEET_NAME = "Drzewa";
    public static final String PROJECT_SHEET_NAME = "Projekt";
    public static final String WORKS_SHEET_NAME = "Prace";
    public static final String MONITORING_SHEET_NAME = "Monitoring";
    public static final String STANDS_SHEET_NAME = "Grupy drzew";
    private static final DateTimeFormatter EXPORT_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.of("Europe/Warsaw"));

    public ExportedFile export(ProjectExportData data) {
        ByteArrayOutputStream xlsxBytes = new ByteArrayOutputStream();
        try (Workbook workbook = new Workbook(xlsxBytes, "ArboMachine", "1.0")) {
            writeTreesSheet(workbook.newWorksheet(TREES_SHEET_NAME), data);
            writeWorksSheet(workbook.newWorksheet(WORKS_SHEET_NAME), data);
            writeMonitoringSheet(workbook.newWorksheet(MONITORING_SHEET_NAME), data);
            writeStandsSheet(workbook.newWorksheet(STANDS_SHEET_NAME), data);
            writeProjectSheet(workbook.newWorksheet(PROJECT_SHEET_NAME), data);
        } catch (IOException writingError) {
            throw new UncheckedIOException(writingError);
        }
        return new ExportedFile(xlsxBytes.toByteArray(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ExportedFile.fileNameOf(data, "xlsx"));
    }

    private static void writeTreesSheet(Worksheet sheet, ProjectExportData data) {
        List<TreeExchangeField> columns = TreeExchangeField.TABULAR_EXPORT_COLUMNS;
        for (int column = 0; column < columns.size(); column++) {
            TreeExchangeField field = columns.get(column);
            sheet.value(0, column, field.polishLabel());
            sheet.style(0, column).bold().wrapText(true).set();
            sheet.width(column, columnWidthOf(field));
        }
        sheet.freezePane(0, 1);
        int row = 1;
        for (TreeExportData tree : data.trees()) {
            for (int column = 0; column < columns.size(); column++) {
                Object value = tree.valueOf(columns.get(column));
                if (value instanceof Number number) {
                    sheet.value(row, column, number instanceof BigDecimal decimal ? decimal.stripTrailingZeros() : number);
                } else if (value != null) {
                    sheet.value(row, column, value.toString());
                }
            }
            row++;
        }
    }

    /** Arkusz "Prace" (etap 28): jeden wiersz = jedna praca, z numerem i gatunkiem drzewa; kolumny: WorkExchangeColumn. */
    private static void writeWorksSheet(Worksheet sheet, ProjectExportData data) {
        List<WorkExchangeColumn> columns = WorkExchangeColumn.COLUMNS;
        for (int column = 0; column < columns.size(); column++) {
            sheet.value(0, column, columns.get(column).polishLabel());
            sheet.style(0, column).bold().wrapText(true).set();
            sheet.width(column, columns.get(column) == WorkExchangeColumn.WORK_DESCRIPTION ? 40 : 18);
        }
        sheet.freezePane(0, 1);
        int row = 1;
        for (TreeExportData tree : data.trees()) {
            for (TreeWorkExportData work : tree.works()) {
                for (int column = 0; column < columns.size(); column++) {
                    Object value = tree.workValueOf(work, columns.get(column));
                    if (value instanceof Number number) sheet.value(row, column, number);
                    else if (value != null) sheet.value(row, column, value.toString());
                }
                row++;
            }
        }
    }

    /** Arkusz "Monitoring" (etap 29): jeden wiersz = jedna kontrola stanu; kolumny: MonitoringExchangeColumn. */
    private static void writeMonitoringSheet(Worksheet sheet, ProjectExportData data) {
        List<MonitoringExchangeColumn> columns = MonitoringExchangeColumn.COLUMNS;
        for (int column = 0; column < columns.size(); column++) {
            sheet.value(0, column, columns.get(column).polishLabel());
            sheet.style(0, column).bold().wrapText(true).set();
            sheet.width(column, columns.get(column) == MonitoringExchangeColumn.NOTES ? 50 : 18);
        }
        sheet.freezePane(0, 1);
        int row = 1;
        for (TreeExportData tree : data.trees()) {
            for (TreeMonitoringExportData record : tree.monitoringRecords()) {
                for (int column = 0; column < columns.size(); column++) {
                    Object value = tree.monitoringValueOf(record, columns.get(column));
                    if (value instanceof Number number) sheet.value(row, column, number);
                    else if (value != null) sheet.value(row, column, value.toString());
                }
                row++;
            }
        }
    }

    /** Arkusz "Grupy drzew" (etap 29): nazwa, pole, liczba i numery drzew, uwagi, zrodlo. */
    private static void writeStandsSheet(Worksheet sheet, ProjectExportData data) {
        String[] headers = {"ID grupy (ArboMachine)", "Nazwa", "Powierzchnia [m²]", "Liczba drzew", "Numery drzew", "Uwagi", "Źródło"};
        for (int column = 0; column < headers.length; column++) {
            sheet.value(0, column, headers[column]);
            sheet.style(0, column).bold().wrapText(true).set();
            sheet.width(column, column == 4 || column == 5 ? 40 : 18);
        }
        sheet.freezePane(0, 1);
        int row = 1;
        for (TreeStandExportData stand : data.stands()) {
            sheet.value(row, 0, stand.standId());
            sheet.value(row, 1, stand.name());
            if (stand.areaSquareMeters() != null) sheet.value(row, 2, stand.areaSquareMeters());
            sheet.value(row, 3, stand.treeInventoryNumbers().size());
            if (stand.treeNumbersText() != null) sheet.value(row, 4, stand.treeNumbersText());
            if (stand.notes() != null) sheet.value(row, 5, stand.notes());
            sheet.value(row, 6, "IMPORT".equals(stand.source()) ? "Import z pliku" : "Narysowana w aplikacji");
            row++;
        }
    }

    private static void writeProjectSheet(Worksheet sheet, ProjectExportData data) {
        String[][] rows = {
                {"Nazwa projektu", data.projectName()},
                {"Zleceniodawca", data.clientName() == null ? "" : data.clientName()},
                {"Adres obiektu", data.siteAddress() == null ? "" : data.siteAddress()},
                {"Data zlecenia", data.orderDate() == null ? "" : data.orderDate().toString()},
                {"Opis", data.projectDescription() == null ? "" : data.projectDescription()},
                {"Data eksportu", EXPORT_TIME.format(data.exportedAt())},
                {"Układ współrzędnych", "WGS 84 (EPSG:4326)"},
                {"Wersja schematu ArboMachine", TreeExchangeField.SCHEMA_VERSION},
        };
        for (int row = 0; row < rows.length; row++) {
            sheet.value(row, 0, rows[row][0]);
            sheet.style(row, 0).bold().set();
            sheet.value(row, 1, rows[row][1]);
        }
        sheet.value(rows.length, 0, "Liczba drzew");
        sheet.style(rows.length, 0).bold().set();
        sheet.value(rows.length, 1, data.trees().size());
        sheet.value(rows.length + 1, 0, "Drzewa bez lokalizacji");
        sheet.style(rows.length + 1, 0).bold().set();
        sheet.value(rows.length + 1, 1, data.treesWithoutLocationCount());
        sheet.width(0, 30);
        sheet.width(1, 60);
    }

    private static double columnWidthOf(TreeExchangeField field) {
        return switch (field) {
            case NOTES, CONDITION_NOTES, HAZARD_NOTES, WORK_NAMES -> 40;
            case SPECIES_POLISH, SPECIES_LATIN -> 24;
            case LATITUDE, LONGITUDE -> 16;
            default -> 14;
        };
    }
}
