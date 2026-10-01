package com.example.arbomachine.domain.dataexchange.imports;

import com.example.arbomachine.domain.dataexchange.schema.TreeExchangeField;
import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.tree.entity.TreeEditableAttributes;
import com.example.arbomachine.domain.tree.entity.TreeLocation;
import com.example.arbomachine.domain.tree.entity.TreeLocationSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Walidacja jednego wiersza importu - TE SAME zasady co formularz drzewa (CreateTreeRequest): numer wymagany do 100
 * znakow, gatunki do 255, obwod liczba calkowita 1-5000 cm, wysokosc (0, 150] m i srednica korony (0, 200] m
 * z max. 2 miejscami po przecinku, lokalizacja WGS84 kompletna albo pusta. Liczby z przecinkiem albo kropka
 * dziesietna (bez separatorow tysiecy). Wynik: gotowe TreeEditableAttributes + TreeLocation albo lista bledow.
 */
final class TreeImportRowValidator {

    private static final Pattern DECIMAL_NUMBER = Pattern.compile("[-+]?\\d+([.,]\\d+)?");

    private TreeImportRowValidator() {
    }

    record RowValidationResult(ValidatedTreeRow validRow, List<TreeImportIssue> errors) {}

    static RowValidationResult validate(ImportedTreeRow row) {
        List<TreeImportIssue> errors = new ArrayList<>(row.readingIssues());
        int rowNumber = row.rowNumber();

        String inventoryNumber = text(row.cell(TreeExchangeField.INVENTORY_NUMBER), Tree.MAXIMUM_INVENTORY_NUMBER_LENGTH, rowNumber, errors);
        if (inventoryNumber == null && errors.stream().noneMatch(error -> TreeExchangeField.INVENTORY_NUMBER.technicalName().equals(error.field()))) {
            errors.add(TreeImportIssue.ofCell(rowNumber, row.cell(TreeExchangeField.INVENTORY_NUMBER), TreeImportIssueCode.REQUIRED_VALUE_MISSING));
        }
        String speciesPolish = text(row.cell(TreeExchangeField.SPECIES_POLISH), Tree.MAXIMUM_SPECIES_POLISH_LENGTH, rowNumber, errors);
        String speciesLatin = text(row.cell(TreeExchangeField.SPECIES_LATIN), Tree.MAXIMUM_SPECIES_LATIN_LENGTH, rowNumber, errors);
        BigDecimal circumference = number(row.cell(TreeExchangeField.TRUNK_CIRCUMFERENCE_CM), 0,
                BigDecimal.valueOf(Tree.MINIMUM_TRUNK_CIRCUMFERENCE_CM), true, BigDecimal.valueOf(Tree.MAXIMUM_TRUNK_CIRCUMFERENCE_CM), rowNumber, errors);
        BigDecimal heightM = number(row.cell(TreeExchangeField.HEIGHT_M), Tree.MEASUREMENT_IN_METERS_SCALE,
                BigDecimal.ZERO, false, new BigDecimal(Tree.MAXIMUM_HEIGHT_M), rowNumber, errors);
        BigDecimal crownDiameterM = number(row.cell(TreeExchangeField.CROWN_DIAMETER_M), Tree.MEASUREMENT_IN_METERS_SCALE,
                BigDecimal.ZERO, false, new BigDecimal(Tree.MAXIMUM_CROWN_DIAMETER_M), rowNumber, errors);
        String notes = text(row.cell(TreeExchangeField.NOTES), Integer.MAX_VALUE, rowNumber, errors);
        String conditionNotes = text(row.cell(TreeExchangeField.CONDITION_NOTES), Integer.MAX_VALUE, rowNumber, errors);
        String hazardNotes = text(row.cell(TreeExchangeField.HAZARD_NOTES), Integer.MAX_VALUE, rowNumber, errors);
        TreeLocation location = location(row, rowNumber, errors);

        if (!errors.isEmpty()) return new RowValidationResult(null, errors);
        return new RowValidationResult(new ValidatedTreeRow(rowNumber, new TreeEditableAttributes(inventoryNumber, speciesLatin,
                speciesPolish, circumference == null ? null : circumference.intValueExact(),
                metersOrNull(heightM), metersOrNull(crownDiameterM), notes, conditionNotes, hazardNotes), location), List.of());
    }

    private static String text(ImportedCell cell, int maximumLength, int rowNumber, List<TreeImportIssue> errors) {
        if (cell.isBlank()) return null;
        String trimmed = cell.rawValue().trim();
        if (trimmed.length() > maximumLength) {
            errors.add(TreeImportIssue.ofCell(rowNumber, cell, TreeImportIssueCode.TEXT_TOO_LONG));
            return null;
        }
        return trimmed;
    }

    /** Liczba z komorki albo null (pusta). minimumInclusive=false -> wartosc musi byc wieksza od minimum. */
    private static BigDecimal number(ImportedCell cell, int maximumScale, BigDecimal minimum, boolean minimumInclusive,
                                     BigDecimal maximum, int rowNumber, List<TreeImportIssue> errors) {
        BigDecimal value = parsedNumberOrNull(cell, rowNumber, errors);
        if (value == null) return null;
        if (value.stripTrailingZeros().scale() > maximumScale) {
            errors.add(TreeImportIssue.ofCell(rowNumber, cell,
                    maximumScale == 0 ? TreeImportIssueCode.NOT_A_WHOLE_NUMBER : TreeImportIssueCode.TOO_MANY_DECIMAL_PLACES));
            return null;
        }
        int comparedToMinimum = value.compareTo(minimum);
        if ((minimumInclusive ? comparedToMinimum < 0 : comparedToMinimum <= 0) || value.compareTo(maximum) > 0) {
            errors.add(TreeImportIssue.ofCell(rowNumber, cell, TreeImportIssueCode.VALUE_OUT_OF_RANGE));
            return null;
        }
        return value.stripTrailingZeros();
    }

    private static BigDecimal parsedNumberOrNull(ImportedCell cell, int rowNumber, List<TreeImportIssue> errors) {
        if (cell.isBlank()) return null;
        String normalized = cell.rawValue().replace(' ', ' ').replace(" ", "").trim();
        if (!DECIMAL_NUMBER.matcher(normalized).matches()) {
            errors.add(TreeImportIssue.ofCell(rowNumber, cell, TreeImportIssueCode.INVALID_NUMBER));
            return null;
        }
        return new BigDecimal(normalized.replace(',', '.'));
    }

    private static TreeLocation location(ImportedTreeRow row, int rowNumber, List<TreeImportIssue> errors) {
        ImportedCell latitudeCell = row.cell(TreeExchangeField.LATITUDE);
        ImportedCell longitudeCell = row.cell(TreeExchangeField.LONGITUDE);
        if (latitudeCell.isBlank() && longitudeCell.isBlank()) return null;
        if (latitudeCell.isBlank() || longitudeCell.isBlank()) {
            errors.add(TreeImportIssue.ofCell(rowNumber, latitudeCell.isBlank() ? latitudeCell : longitudeCell,
                    TreeImportIssueCode.INCOMPLETE_LOCATION));
            return null;
        }
        int errorCountBefore = errors.size();
        BigDecimal latitude = coordinate(latitudeCell, TreeLocation.MINIMUM_LATITUDE, TreeLocation.MAXIMUM_LATITUDE, rowNumber, errors);
        BigDecimal longitude = coordinate(longitudeCell, TreeLocation.MINIMUM_LONGITUDE, TreeLocation.MAXIMUM_LONGITUDE, rowNumber, errors);
        return errors.size() > errorCountBefore ? null : TreeLocation.of(latitude, longitude, null, null, TreeLocationSource.IMPORT);
    }

    private static BigDecimal coordinate(ImportedCell cell, String minimum, String maximum, int rowNumber, List<TreeImportIssue> errors) {
        BigDecimal value = parsedNumberOrNull(cell, rowNumber, errors);
        if (value == null) return null;
        if (value.compareTo(new BigDecimal(minimum)) < 0 || value.compareTo(new BigDecimal(maximum)) > 0) {
            errors.add(TreeImportIssue.ofCell(rowNumber, cell, TreeImportIssueCode.VALUE_OUT_OF_RANGE));
            return null;
        }
        return value;
    }

    /** Skala kolumny numeric(5,2) - jak w formularzu drzewa. */
    private static BigDecimal metersOrNull(BigDecimal meters) {
        return meters == null ? null : meters.setScale(Tree.MEASUREMENT_IN_METERS_SCALE, RoundingMode.UNNECESSARY);
    }
}
