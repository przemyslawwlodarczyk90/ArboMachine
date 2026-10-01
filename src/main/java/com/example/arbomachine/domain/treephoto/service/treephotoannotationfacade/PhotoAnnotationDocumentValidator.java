package com.example.arbomachine.domain.treephoto.service.treephotoannotationfacade;

import com.example.arbomachine.domain.treephoto.annotation.PhotoAnnotationColor;
import com.example.arbomachine.domain.treephoto.annotation.PhotoAnnotationElementType;
import com.example.arbomachine.domain.treephoto.annotation.PhotoAnnotationLimits;
import com.example.arbomachine.domain.treephoto.annotation.PhotoAnnotationStrokeSize;
import com.example.arbomachine.domain.treephoto.dto.NormalizedPointDto;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationDocumentRequest;
import com.example.arbomachine.domain.treephoto.dto.PhotoAnnotationElementDto;
import com.example.arbomachine.domain.treephoto.exception.InvalidPhotoAnnotationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Walidacja i normalizacja dokumentu adnotacji V1 - do bazy trafia wylacznie wynik tej metody, nigdy surowy JSON.
 * Sprawdza: wersje schematu, liczbe elementow, identyfikatory, typy, kolory i grubosci z katalogu, wspolrzedne 0..1,
 * liczbe punktow wg typu, tekst (tylko TEXT) i laczny limit punktow. Normalizuje: tekst przyciety, wspolrzedne do
 * 6 miejsc po przecinku (dokladnosc duzo ponizej piksela), tekst usuwany z elementow innych niz TEXT.
 */
final class PhotoAnnotationDocumentValidator {

    private static final Pattern ELEMENT_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int COORDINATE_SCALE = 6;

    private PhotoAnnotationDocumentValidator() {
    }

    static PhotoAnnotationDocumentRequest validatedDocumentOf(PhotoAnnotationDocumentRequest document) {
        if (document == null) throw invalid("Annotation document is mandatory");
        if (document.schemaVersion() == null || document.schemaVersion() != PhotoAnnotationLimits.CURRENT_SCHEMA_VERSION) {
            throw invalid("Unsupported annotation schema version");
        }
        List<PhotoAnnotationElementDto> elements = document.elements();
        if (elements == null || elements.isEmpty()) throw invalid("Annotation must contain at least one element");
        if (elements.size() > PhotoAnnotationLimits.MAXIMUM_ELEMENT_COUNT) throw invalid("Too many annotation elements");

        Set<String> seenElementIds = new HashSet<>();
        int totalPointCount = 0;
        List<PhotoAnnotationElementDto> validatedElements = new ArrayList<>();
        for (PhotoAnnotationElementDto element : elements) {
            if (element == null) throw invalid("Invalid annotation element");
            String elementId = element.id() == null ? "" : element.id().trim();
            if (elementId.isEmpty() || elementId.length() > PhotoAnnotationLimits.MAXIMUM_ELEMENT_ID_LENGTH
                    || !ELEMENT_ID_PATTERN.matcher(elementId).matches()) {
                throw invalid("Invalid annotation element id");
            }
            if (!seenElementIds.add(elementId)) throw invalid("Duplicate annotation element id");
            PhotoAnnotationElementType type = enumValueOf(PhotoAnnotationElementType.class, element.type(),
                    "Unsupported annotation element type");
            PhotoAnnotationColor color = enumValueOf(PhotoAnnotationColor.class, element.color(), "Unsupported annotation color");
            PhotoAnnotationStrokeSize strokeSize = enumValueOf(PhotoAnnotationStrokeSize.class, element.strokeSize(),
                    "Unsupported annotation stroke size");
            List<NormalizedPointDto> points = validatedPointsOf(element.points(), type);
            totalPointCount += points.size();
            if (totalPointCount > PhotoAnnotationLimits.MAXIMUM_TOTAL_POINT_COUNT) throw invalid("Too many annotation points");
            validatedElements.add(new PhotoAnnotationElementDto(elementId, type.name(), points, color.name(),
                    strokeSize.name(), validatedTextOf(element.text(), type)));
        }
        return new PhotoAnnotationDocumentRequest(PhotoAnnotationLimits.CURRENT_SCHEMA_VERSION, validatedElements);
    }

    private static List<NormalizedPointDto> validatedPointsOf(List<NormalizedPointDto> points, PhotoAnnotationElementType type) {
        int pointCount = points == null ? 0 : points.size();
        boolean hasValidPointCount = switch (type) {
            case LINE, ARROW -> pointCount == 2;
            case TEXT -> pointCount == 1;
            case FREEHAND -> pointCount >= 2 && pointCount <= PhotoAnnotationLimits.MAXIMUM_FREEHAND_POINT_COUNT;
        };
        if (!hasValidPointCount) throw invalid("Invalid number of annotation points");
        List<NormalizedPointDto> validatedPoints = new ArrayList<>(pointCount);
        for (NormalizedPointDto point : points) {
            if (point == null || !isNormalized(point.x()) || !isNormalized(point.y())) {
                throw invalid("Annotation point coordinates must be between 0 and 1");
            }
            validatedPoints.add(new NormalizedPointDto(point.x().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP),
                    point.y().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP)));
        }
        return validatedPoints;
    }

    private static String validatedTextOf(String text, PhotoAnnotationElementType type) {
        if (type != PhotoAnnotationElementType.TEXT) return null;
        String trimmedText = text == null ? "" : text.trim();
        if (trimmedText.isEmpty()) throw invalid("Annotation text is mandatory");
        if (trimmedText.length() > PhotoAnnotationLimits.MAXIMUM_TEXT_LENGTH) {
            throw invalid("Annotation text must have at most 500 characters");
        }
        return trimmedText;
    }

    private static boolean isNormalized(BigDecimal coordinate) {
        return coordinate != null && coordinate.compareTo(BigDecimal.ZERO) >= 0 && coordinate.compareTo(BigDecimal.ONE) <= 0;
    }

    private static <E extends Enum<E>> E enumValueOf(Class<E> enumType, String value, String errorMessage) {
        return Arrays.stream(enumType.getEnumConstants())
                .filter(constant -> constant.name().equals(value))
                .findFirst()
                .orElseThrow(() -> invalid(errorMessage));
    }

    private static InvalidPhotoAnnotationException invalid(String message) {
        return new InvalidPhotoAnnotationException(message);
    }
}
