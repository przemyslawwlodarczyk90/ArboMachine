package com.example.arbomachine.domain.projectdocument.generator;

import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentLocationData;
import com.example.arbomachine.domain.projectdocument.data.ProjectDocumentTreeData;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formatowanie wartosci w szablonie dokumentu (w szablonie jako "format"): polski przecinek dziesietny,
 * jednostki, daty w strefie Europe/Warsaw i "—" zamiast pustych wartosci (nigdy "null"). Bez logiki domenowej.
 */
public final class ProjectReportFormatter {

    /** Brak wartosci w tabelach i polach pomiarow. */
    public static final String MISSING_VALUE = "—";

    /** V1: daty dokumentu w czasie polskim (aplikacja dla polskiego rynku; brak strefy w profilu uzytkownika). */
    private static final ZoneId DOCUMENT_TIME_ZONE = ZoneId.of("Europe/Warsaw");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm").withZone(DOCUMENT_TIME_ZONE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(DOCUMENT_TIME_ZONE);

    private static DecimalFormat polishDecimalFormat(String pattern) {
        return new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.of("pl", "PL")));
    }

    public String text(String value) {
        return hasText(value) ? value.trim() : MISSING_VALUE;
    }

    public boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String circumference(Integer trunkCircumferenceCm) {
        return trunkCircumferenceCm == null ? MISSING_VALUE : trunkCircumferenceCm + " cm";
    }

    /** 18.50 -> "18,5 m", 9.00 -> "9 m". */
    public String meters(BigDecimal valueInMeters) {
        return valueInMeters == null ? MISSING_VALUE : polishDecimalFormat("0.##").format(valueInMeters) + " m";
    }

    /** Liczba bez jednostki - do kolumn tabeli z jednostka w naglowku. */
    public String number(Number value) {
        return value == null ? MISSING_VALUE : polishDecimalFormat("0.##").format(value);
    }

    /** "52,2297° N, 21,0122° E" - WGS84, do 7 miejsc jak w bazie (bez zbednych zer). */
    public String coordinates(ProjectDocumentLocationData location) {
        if (location == null) return "Nie podano";
        DecimalFormat coordinateFormat = polishDecimalFormat("0.0######");
        BigDecimal latitude = location.latitude();
        BigDecimal longitude = location.longitude();
        return coordinateFormat.format(latitude.abs()) + "° " + (latitude.signum() < 0 ? "S" : "N") + ", "
                + coordinateFormat.format(longitude.abs()) + "° " + (longitude.signum() < 0 ? "W" : "E");
    }

    /** "GPS urządzenia, dokładność ±4 m" / "wskazanie na mapie" / null (brak danych pomiaru - nic nie pokazujemy). */
    public String locationMeasurement(ProjectDocumentLocationData location) {
        if (location == null) return null;
        String source = switch (location.source() == null ? "" : location.source()) {
            case "DEVICE_GNSS" -> "GPS urządzenia";
            case "MAP" -> "wskazanie na mapie";
            case "MANUAL" -> "współrzędne wpisane ręcznie";
            case "IMPORT" -> "import z pliku";
            default -> null;
        };
        String accuracy = location.accuracyM() == null ? null
                : "dokładność ±" + polishDecimalFormat("0.#").format(location.accuracyM()) + " m";
        if (source == null && accuracy == null) return null;
        if (source == null) return accuracy;
        return accuracy == null ? source : source + ", " + accuracy;
    }

    /** "lipa drobnolistna (Tilia cordata)", jedna z nazw albo "Gatunek nie podany". */
    public String species(ProjectDocumentTreeData tree) {
        boolean hasPolish = hasText(tree.speciesPolish());
        boolean hasLatin = hasText(tree.speciesLatin());
        if (hasPolish && hasLatin) return tree.speciesPolish().trim() + " (" + tree.speciesLatin().trim() + ")";
        if (hasPolish) return tree.speciesPolish().trim();
        if (hasLatin) return tree.speciesLatin().trim();
        return "Gatunek nie podany";
    }

    public String dateTime(Instant instant) {
        return instant == null ? MISSING_VALUE : DATE_TIME.format(instant);
    }

    public String date(Instant instant) {
        return instant == null ? MISSING_VALUE : DATE.format(instant);
    }
}
