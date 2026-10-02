package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.schema.WorkExchangeColumn;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV prac projektu (etap 28) - jeden wiersz = jedna praca (drzewo moze miec wiele prac), ten sam format co CSV
 * drzew: UTF-8 z BOM, separator ";", CRLF, naglowki po polsku (WorkExchangeColumn).
 */
@Component
public class TreeWorksCsvExporter {

    public ExportedFile export(ProjectExportData data) {
        ByteArrayOutputStream csvBytes = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(csvBytes, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, TreeCsvExporter.CSV_FORMAT)) {
            writer.write("\uFEFF");
            printer.printRecord(WorkExchangeColumn.COLUMNS.stream().map(WorkExchangeColumn::polishLabel).toList());
            for (TreeExportData tree : data.trees()) {
                for (TreeWorkExportData work : tree.works()) {
                    List<String> record = new ArrayList<>();
                    for (WorkExchangeColumn column : WorkExchangeColumn.COLUMNS) {
                        record.add(TreeCsvExporter.csvValueOf(tree.workValueOf(work, column)));
                    }
                    printer.printRecord(record);
                }
            }
        } catch (IOException writingError) {
            throw new UncheckedIOException(writingError);
        }
        return new ExportedFile(csvBytes.toByteArray(), "text/csv; charset=UTF-8",
                "arbomachine-projekt-" + data.projectId() + "-prace.csv");
    }
}
