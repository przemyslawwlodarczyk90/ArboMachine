package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.imports.TabularTreeImportRows.NumberedRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV drzew: wymagane UTF-8 (z BOM albo bez - inne kodowanie -> INVALID_ENCODING zamiast "krzakow" w danych),
 * separator wykrywany z wiersza naglowka (";" jak w eksporcie ArboMachine, "," albo tabulator). Parsowanie
 * (cudzyslowy, separator i nowe linie w polach) robi Commons CSV - nigdy split po separatorze.
 */
@Component
public class CsvTreeImportParser implements TreeImportParser {

    @Override
    public TreeImportFormat format() {
        return TreeImportFormat.CSV;
    }

    @Override
    public ParsedTreeImportFile parse(byte[] content) {
        String text = strictUtf8Of(content);
        if (text.startsWith("﻿")) text = text.substring(1);
        if (text.isBlank()) throw new TreeImportFileException(TreeImportIssueCode.EMPTY_FILE);
        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiterOf(text)).get();
        List<NumberedRow> rows = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            int rowNumber = 0;
            for (CSVRecord record : parser) {
                rowNumber++;
                rows.add(new NumberedRow(rowNumber, record.toList()));
            }
        } catch (UncheckedIOException | IllegalStateException malformedCsv) {
            throw new TreeImportFileException(TreeImportIssueCode.UNREADABLE_FILE);
        } catch (IOException readingError) {
            throw new TreeImportFileException(TreeImportIssueCode.UNREADABLE_FILE);
        }
        return TabularTreeImportRows.parse(rows);
    }

    private static String strictUtf8Of(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException notUtf8) {
            throw new TreeImportFileException(TreeImportIssueCode.INVALID_ENCODING);
        }
    }

    /** Separator, ktorego jest najwiecej w pierwszej linii (poza cudzyslowami nie liczymy - naglowki ich nie maja). */
    static char delimiterOf(String text) {
        int lineEnd = text.indexOf('\n');
        String headerLine = lineEnd < 0 ? text : text.substring(0, lineEnd);
        char bestDelimiter = ';';
        long bestCount = -1;
        for (char candidate : new char[]{';', ',', '\t'}) {
            long count = headerLine.chars().filter(character -> character == candidate).count();
            if (count > bestCount) {
                bestCount = count;
                bestDelimiter = candidate;
            }
        }
        return bestDelimiter;
    }
}
