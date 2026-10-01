package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.export.TreeXlsxExporter;
import com.example.arbomachine.domain.dataexchange.imports.TabularTreeImportRows.NumberedRow;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * XLSX drzew: arkusz "Drzewa" (jak w eksporcie ArboMachine), a gdy go nie ma - pierwszy arkusz. Liczby z komorek
 * liczbowych jako zapis dziesietny (bez utraty precyzji), tekst bez zmian. Stary format .xls nie jest obslugiwany.
 */
@Component
public class XlsxTreeImportParser implements TreeImportParser {

    @Override
    public TreeImportFormat format() {
        return TreeImportFormat.XLSX;
    }

    @Override
    public ParsedTreeImportFile parse(byte[] content) {
        if (content.length < 4 || !ReadableWorkbook.isOOXMLZipHeader(content)) {
            throw new TreeImportFileException(TreeImportIssueCode.UNREADABLE_FILE);
        }
        List<NumberedRow> rows = new ArrayList<>();
        try (ReadableWorkbook workbook = new ReadableWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.findSheet(TreeXlsxExporter.TREES_SHEET_NAME).orElse(workbook.getFirstSheet());
            try (Stream<Row> sheetRows = sheet.openStream()) {
                sheetRows.forEach(row -> rows.add(new NumberedRow(row.getRowNum(), valuesOf(row))));
            }
        } catch (IOException | RuntimeException unreadableWorkbook) {
            throw new TreeImportFileException(TreeImportIssueCode.UNREADABLE_FILE);
        }
        return TabularTreeImportRows.parse(rows);
    }

    private static List<String> valuesOf(Row row) {
        List<String> values = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < row.getCellCount(); columnIndex++) {
            Cell cell = row.getCell(columnIndex);
            values.add(cell == null ? null : textOf(cell));
        }
        return values;
    }

    private static String textOf(Cell cell) {
        return switch (cell.getType()) {
            case NUMBER -> cell.asNumber().stripTrailingZeros().toPlainString();
            case EMPTY -> null;
            case STRING -> cell.asString();
            default -> cell.getText();
        };
    }
}
