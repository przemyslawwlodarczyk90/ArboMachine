package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportPreviewRowResponse;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportUpload;
import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.imports.TreeImportRowValidator.RowValidationResult;
import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.infrastructure.repository.TreeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Analiza pliku importu dla WLASNEGO projektu (wlasnosc sprawdza handler): format po rozszerzeniu, limity techniczne,
 * odczyt (parser formatu), walidacja wierszy, ostrzezenia o duplikatach numerow. Nic nie zapisuje - ten sam wynik
 * sluzy do "Sprawdz plik" i do ponownego sprawdzenia przy imporcie (plik wysylany drugi raz, bez stanu na serwerze).
 */
@Component
public class TreeImportAnalyzer {

    /** Limity techniczne V1 (docs/DO_DOPRECYZOWANIA.md). */
    public static final int MAXIMUM_FILE_SIZE_BYTES = 15 * 1024 * 1024;
    public static final int MAXIMUM_ROW_COUNT = 2000;
    static final int PREVIEW_ROW_COUNT = 10;

    private final Map<TreeImportFormat, TreeImportParser> parserByFormat;
    private final TreeRepository treeRepository;

    public TreeImportAnalyzer(List<TreeImportParser> parsers, TreeRepository treeRepository) {
        this.parserByFormat = parsers.stream().collect(Collectors.toMap(TreeImportParser::format, Function.identity()));
        this.treeRepository = treeRepository;
    }

    public record TreeImportAnalysis(TreeImportValidationResponse response, List<ValidatedTreeRow> validRows) {}

    public TreeImportAnalysis analyze(Project ownedProject, TreeImportUpload upload) {
        String fileName = upload.originalFileName() == null ? "" : upload.originalFileName();
        Optional<TreeImportFormat> format = TreeImportFormat.fromFileName(fileName);
        if (format.isEmpty()) return rejectedFile(fileName, null, TreeImportIssueCode.UNSUPPORTED_FILE_FORMAT);
        if (upload.content() == null || upload.content().length == 0) return rejectedFile(fileName, format.get(), TreeImportIssueCode.EMPTY_FILE);
        if (upload.content().length > MAXIMUM_FILE_SIZE_BYTES) return rejectedFile(fileName, format.get(), TreeImportIssueCode.FILE_TOO_LARGE);

        ParsedTreeImportFile parsedFile;
        try {
            parsedFile = parserByFormat.get(format.get()).parse(upload.content());
        } catch (TreeImportFileException fileError) {
            return new TreeImportAnalysis(responseWithFileError(fileName, format.get(), fileError.toIssue()), List.of());
        }
        List<ImportedTreeRow> rows = parsedFile.rows().stream().filter(row -> !row.isEmpty()).toList();
        if (rows.isEmpty()) return rejectedFile(fileName, format.get(), TreeImportIssueCode.NO_DATA_ROWS);
        if (rows.size() > MAXIMUM_ROW_COUNT) return rejectedFile(fileName, format.get(), TreeImportIssueCode.TOO_MANY_ROWS);

        List<ValidatedTreeRow> validRows = new ArrayList<>();
        List<TreeImportIssue> errors = new ArrayList<>();
        for (ImportedTreeRow row : rows) {
            RowValidationResult result = TreeImportRowValidator.validate(row);
            if (result.validRow() != null) validRows.add(result.validRow());
            errors.addAll(result.errors());
        }
        List<TreeImportIssue> warnings = new ArrayList<>(parsedFile.fileWarnings());
        warnings.addAll(duplicateWarningsOf(ownedProject, validRows));
        int invalidRowCount = rows.size() - validRows.size();
        TreeImportValidationResponse response = new TreeImportValidationResponse(fileName, format.get().name(), rows.size(),
                validRows.size(), invalidRowCount, errors.isEmpty() && !validRows.isEmpty(), List.of(), errors, warnings,
                parsedFile.ignoredColumns(), validRows.stream().limit(PREVIEW_ROW_COUNT).map(TreeImportAnalyzer::previewOf).toList());
        return new TreeImportAnalysis(response, validRows);
    }

    /**
     * V1 nie scala drzew: kazdy wiersz tworzy NOWE drzewo, takze gdy numer juz istnieje (zasady unikalnosci numerow
     * nie sa ustalone). Duplikaty sa tylko zglaszane, zeby uzytkownik swiadomie zdecydowal.
     */
    private List<TreeImportIssue> duplicateWarningsOf(Project ownedProject, List<ValidatedTreeRow> validRows) {
        Set<String> existingNumbers = treeRepository.findAllByProjectIdOrderByCreatedAtAscIdAsc(ownedProject.getId()).stream()
                .map(Tree::getInventoryNumber).collect(Collectors.toSet());
        Set<String> numbersInFile = new HashSet<>();
        List<TreeImportIssue> warnings = new ArrayList<>();
        for (ValidatedTreeRow row : validRows) {
            String inventoryNumber = row.attributes().inventoryNumber();
            ImportedCell numberCell = new ImportedCell(TreeExchangeField.INVENTORY_NUMBER,
                    TreeExchangeField.INVENTORY_NUMBER.polishLabel(), inventoryNumber);
            if (existingNumbers.contains(inventoryNumber)) {
                warnings.add(TreeImportIssue.ofCell(row.rowNumber(), numberCell, TreeImportIssueCode.INVENTORY_NUMBER_EXISTS_IN_PROJECT));
            }
            if (!numbersInFile.add(inventoryNumber)) {
                warnings.add(TreeImportIssue.ofCell(row.rowNumber(), numberCell, TreeImportIssueCode.DUPLICATE_INVENTORY_NUMBER_IN_FILE));
            }
        }
        return warnings;
    }

    private static TreeImportPreviewRowResponse previewOf(ValidatedTreeRow row) {
        return new TreeImportPreviewRowResponse(row.rowNumber(), row.attributes().inventoryNumber(), row.attributes().speciesPolish(),
                row.attributes().speciesLatin(), row.attributes().trunkCircumferenceCm(), row.attributes().heightM(),
                row.attributes().crownDiameterM(), row.locationOrNull() != null);
    }

    private static TreeImportAnalysis rejectedFile(String fileName, TreeImportFormat format, TreeImportIssueCode code) {
        return new TreeImportAnalysis(responseWithFileError(fileName, format, TreeImportIssue.ofFile(code, null)), List.of());
    }

    private static TreeImportValidationResponse responseWithFileError(String fileName, TreeImportFormat format, TreeImportIssue fileError) {
        return new TreeImportValidationResponse(fileName, format == null ? null : format.name(), 0, 0, 0, false,
                List.of(fileError), List.of(), List.of(), List.of(), List.of());
    }
}
