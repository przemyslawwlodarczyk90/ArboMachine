package com.example.arbomachine.domain.dataexchange.schema;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Typowe naglowki arkuszy z aplikacji terenowych do inwentaryzacji drzew (etap 29) - dodatkowe nazwy kolumn
 * rozpoznawane przy imporcie CSV/XLSX (obok nazw z TreeExchangeField). Profil V1 na podstawie przykladowych plikow
 * arborysty; do rozszerzania bez zmian w walidacji i zapisie drzew.
 * <ul>
 *   <li>proste aliasy (np. "Etykieta" = numer drzewa, "Gatunki (lac.)" = gatunek lacinski),</li>
 *   <li>kolumny karty oceny drzewa (odziomek, pien, konary...) - laczone w tekst "Stan drzewa" jako "Kolumna: wartosc",</li>
 *   <li>wiersz z wypelniona powierzchnia obszaru = obszar (grupa krzewow/drzew), a nie drzewo - pomijany,</li>
 *   <li>obwod wielopniowy ("57+34", "48, 27, 37") - nie przeliczamy go: trafia do notatek, obwod zostaje pusty.</li>
 * </ul>
 */
public final class FieldInventoryColumnAliases {

    private static final Map<String, TreeExchangeField> ALIASES = Map.of(
            "etykieta", TreeExchangeField.INVENTORY_NUMBER,
            "gatunki (lac.)", TreeExchangeField.SPECIES_LATIN,
            "gatunki", TreeExchangeField.SPECIES_POLISH,
            "obwody na 130 cm", TreeExchangeField.TRUNK_CIRCUMFERENCE_CM,
            "pozycja wgs84 - szerokość", TreeExchangeField.LATITUDE,
            "pozycja wgs84 - długość", TreeExchangeField.LONGITUDE,
            "karta drzewa", TreeExchangeField.CONDITION_NOTES);

    /** Kolumny karty oceny drzewa (oraz ich podkolumny "Pień - ubytek wgłębny" itp.). */
    private static final List<String> CONDITION_CARD_COLUMNS = List.of("wartość drzewa", "liczba przewodników", "kształt korony",
            "odziomek", "pień", "konary", "gałęzie", "korzenie", "siedlisko");

    private static final String AREA_COLUMN = "obszar - powierzchnia";
    private static final String MULTI_STEM_SUM_COLUMN_PREFIX = "obwody na 130 cm - suma";
    /** Kilka obwodow: "57+34", "48, 27, 37" albo "48; 27" (rozne arkusze zapisuja to roznie). */
    private static final Pattern MULTI_STEM_CIRCUMFERENCE = Pattern.compile("\\s*\\d+(\\s*[+,;]\\s*\\d+)+\\s*");

    private FieldInventoryColumnAliases() {
    }

    public static Optional<TreeExchangeField> fieldOf(String header) {
        return Optional.ofNullable(ALIASES.get(normalized(header)));
    }

    public static boolean isConditionCardColumn(String header) {
        String normalizedHeader = normalized(header);
        return CONDITION_CARD_COLUMNS.stream().anyMatch(column -> normalizedHeader.equals(column) || normalizedHeader.startsWith(column + " - "));
    }

    public static boolean isAreaColumn(String header) {
        return normalized(header).equals(AREA_COLUMN);
    }

    public static boolean isMultiStemSumColumn(String header) {
        return normalized(header).startsWith(MULTI_STEM_SUM_COLUMN_PREFIX);
    }

    public static boolean isMultiStemCircumference(String value) {
        return value != null && MULTI_STEM_CIRCUMFERENCE.matcher(value).matches();
    }

    private static String normalized(String header) {
        return header == null ? "" : header.replace("﻿", "").trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
