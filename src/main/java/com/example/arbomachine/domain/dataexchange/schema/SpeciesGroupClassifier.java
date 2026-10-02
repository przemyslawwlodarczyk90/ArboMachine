package com.example.arbomachine.domain.dataexchange.schema;

import java.util.Locale;
import java.util.Set;

/**
 * Grupa gatunku drzewa (etap 29) - iglaste / lisciaste - wyprowadzana z nazwy gatunku (rodzaj lacinski albo pierwszy
 * wyraz nazwy polskiej) do stylowania w GIS i symboli na mapie. Lista rodzajow iglastych V1 - do uzgodnienia
 * (np. milorzab traktujemy jak lisciaste). Brak gatunku = brak grupy. Ta sama lista jest we frontendzie (speciesGroup.ts).
 */
public final class SpeciesGroupClassifier {

    public static final String CONIFER = "CONIFER";
    public static final String DECIDUOUS = "DECIDUOUS";

    private static final Set<String> CONIFER_GENERA = Set.of("abies", "picea", "pinus", "larix", "pseudotsuga", "tsuga", "cedrus",
            "taxus", "juniperus", "thuja", "thujopsis", "chamaecyparis", "cupressus", "cupressocyparis", "cuprocyparis", "calocedrus",
            "platycladus", "sequoia", "sequoiadendron", "metasequoia", "taxodium", "cryptomeria", "cunninghamia", "sciadopitys",
            "araucaria", "microbiota", "torreya", "cephalotaxus", "podocarpus");

    private static final Set<String> CONIFER_POLISH_NAMES = Set.of("jodła", "świerk", "sosna", "modrzew", "daglezja", "choina",
            "cedr", "cis", "jałowiec", "żywotnik", "tuja", "żywotnikowiec", "cyprysik", "cyprys", "cyprysowiec", "cypryśnik", "cedrzyniec",
            "biota", "mamutowiec", "sekwoja", "metasekwoja", "kryptomeria", "araukaria", "torreja", "głowocis", "sośnica", "zastrzalin");

    private SpeciesGroupClassifier() {
    }

    /** CONIFER, DECIDUOUS albo null (gatunek nie podany). */
    public static String groupOf(String speciesLatin, String speciesPolish) {
        String latinGenus = firstWord(speciesLatin).replace("×", "").replace("x ", "");
        String polishName = firstWord(speciesPolish);
        if (latinGenus.isEmpty() && polishName.isEmpty()) return null;
        if (CONIFER_GENERA.contains(latinGenus) || CONIFER_POLISH_NAMES.contains(polishName)) return CONIFER;
        return DECIDUOUS;
    }

    private static String firstWord(String text) {
        if (text == null || text.isBlank()) return "";
        String trimmed = text.trim().toLowerCase(Locale.ROOT).replaceFirst("^[×x]\\s*", "");
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }
}
