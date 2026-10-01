package com.example.arbomachine.domain.dataexchange.export;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV drzew V1 dla polskiego Excela/LibreOffice: UTF-8 z BOM (Excel rozpoznaje polskie znaki), separator ";",
 * przecinek dziesietny, konce linii CRLF, naglowki po polsku, puste pole = brak wartosci. Escaping (cudzyslowy,
 * separator i nowe linie w notatkach) robi Commons CSV. Kolumny i kolejnosc: TreeExchangeField.TABULAR_EXPORT_COLUMNS.
 */
@Component
public class TreeCsvExporter {

    public static final char DELIMITER = ';';
    private static final String UTF8_BYTE_ORDER_MARK = "﻿";
    static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(DELIMITER)
            .setRecordSeparator("\r\n")
            .setQuoteMode(QuoteMode.MINIMAL)
            .get();

    public ExportedFile export(ProjectExportData data) {
        ByteArrayOutputStream csvBytes = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(csvBytes, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSV_FORMAT)) {
            writer.write(UTF8_BYTE_ORDER_MARK);
            printer.printRecord(TreeExchangeField.TABULAR_EXPORT_COLUMNS.stream().map(TreeExchangeField::polishLabel).toList());
            for (TreeExportData tree : data.trees()) {
                List<String> record = new ArrayList<>();
                for (TreeExchangeField column : TreeExchangeField.TABULAR_EXPORT_COLUMNS) {
                    record.add(csvValueOf(tree.valueOf(column)));
                }
                printer.printRecord(record);
            }
        } catch (IOException writingError) {
            throw new UncheckedIOException(writingError);
        }
        return new ExportedFile(csvBytes.toByteArray(), "text/csv; charset=UTF-8", ExportedFile.fileNameOf(data, "csv"));
    }

    /** null -> puste pole; liczby dziesietne z przecinkiem i bez zbednych zer (18.50 -> "18,5"). */
    static String csvValueOf(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString().replace('.', ',');
        return value.toString();
    }
}
