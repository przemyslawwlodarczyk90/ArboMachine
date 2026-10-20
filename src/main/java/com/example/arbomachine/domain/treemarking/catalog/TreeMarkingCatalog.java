package com.example.arbomachine.domain.treemarking.catalog;

import com.example.arbomachine.domain.treemarking.exception.UnsupportedTreeMarkingException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Katalog oznaczen drzewa V1 (etap 31) - "dodatkowe symbole graficzne powiazane z parametrami drzewa" z odpowiedzi
 * arborysty (np. drzewo wartosciowe, do usuniecia, zlom). Jedno drzewo moze miec wiele oznaczen naraz.
 * <p>
 * Lista w kodzie, nie w bazie (zmiana bez migracji). Nazwy, skroty i kolory to ZALOZENIE V1 - wlasna lista
 * i symbolika arborysty, edycja katalogu przez uzytkownika oraz oznaczenia wyliczane z danych (waloryzacja, prace)
 * sa w docs/DO_DOPRECYZOWANIA.md. Usuniecie kodu z listy nie usuwa zapisanych oznaczen - API po prostu je pomija.
 */
@Component
public class TreeMarkingCatalog {

    /** Techniczny limit kodu = dlugosc kolumny tree_marking.code. */
    public static final int MAXIMUM_CODE_LENGTH = 64;

    private static final List<TreeMarkingDefinition> MARKINGS = List.of(
            new TreeMarkingDefinition("VALUABLE", "Drzewo wartościowe", TreeMarkingCategory.VALUE, "W", "#2E7D32"),
            new TreeMarkingDefinition("NATURE_MONUMENT", "Pomnik przyrody", TreeMarkingCategory.VALUE, "PP", "#1B5E20"),
            new TreeMarkingDefinition("MONUMENT_CANDIDATE", "Kandydat na pomnik przyrody", TreeMarkingCategory.VALUE, "KP", "#558B2F"),
            new TreeMarkingDefinition("TO_REMOVE", "Do usunięcia", TreeMarkingCategory.MANAGEMENT, "U", "#C62828"),
            new TreeMarkingDefinition("TO_PROTECT", "Do ochrony (np. na czas budowy)", TreeMarkingCategory.MANAGEMENT, "O", "#1565C0"),
            new TreeMarkingDefinition("TO_TRANSPLANT", "Do przesadzenia", TreeMarkingCategory.MANAGEMENT, "P", "#6A1B9A"),
            new TreeMarkingDefinition("OBSERVATION", "Do obserwacji", TreeMarkingCategory.MANAGEMENT, "Ob", "#B07B00"),
            new TreeMarkingDefinition("SNAG", "Złom", TreeMarkingCategory.CONDITION, "Z", "#4E342E"),
            new TreeMarkingDefinition("WINDTHROW", "Wywrot", TreeMarkingCategory.CONDITION, "Wy", "#6D4C41"),
            new TreeMarkingDefinition("DEAD", "Posusz / drzewo martwe", TreeMarkingCategory.CONDITION, "M", "#616161"),
            new TreeMarkingDefinition("HAZARD", "Zagrożenie dla ludzi lub mienia", TreeMarkingCategory.HAZARD, "!", "#D84315"),
            new TreeMarkingDefinition("INFRASTRUCTURE_CONFLICT", "Kolizja z infrastrukturą", TreeMarkingCategory.HAZARD, "K", "#E65100"),
            new TreeMarkingDefinition("HOLLOW", "Dziupla", TreeMarkingCategory.HABITAT, "D", "#00838F"),
            new TreeMarkingDefinition("NEST", "Gniazdo / siedlisko ptaków", TreeMarkingCategory.HABITAT, "G", "#0277BD"),
            new TreeMarkingDefinition("PROTECTED_SPECIES", "Siedlisko gatunku chronionego", TreeMarkingCategory.HABITAT, "Ch", "#00695C"),
            new TreeMarkingDefinition("FUNGI", "Owocniki grzybów", TreeMarkingCategory.HABITAT, "F", "#8D6E63")
    );

    private static final Map<String, Integer> POSITION_BY_CODE = MARKINGS.stream()
            .collect(Collectors.toUnmodifiableMap(TreeMarkingDefinition::code, MARKINGS::indexOf));

    private static final Map<String, TreeMarkingDefinition> MARKING_BY_CODE = MARKINGS.stream()
            .collect(Collectors.toUnmodifiableMap(TreeMarkingDefinition::code, Function.identity()));

    /** Wszystkie oznaczenia w stalej kolejnosci (kolejnosc w UI i na mapie). */
    public List<TreeMarkingDefinition> allMarkings() {
        return MARKINGS;
    }

    public boolean isSupported(String code) {
        return MARKING_BY_CODE.containsKey(code);
    }

    /**
     * Kody z requestu po przycieciu, bez powtorzen; kod spoza katalogu -> UnsupportedTreeMarkingException (HTTP 400),
     * wiec do bazy nie trafia dowolny tekst.
     */
    public Set<String> requireSupportedCodes(Collection<String> requestedCodes) {
        Set<String> codes = new LinkedHashSet<>();
        for (String requestedCode : requestedCodes) {
            String trimmedCode = requestedCode == null ? "" : requestedCode.trim();
            if (!MARKING_BY_CODE.containsKey(trimmedCode)) {
                throw new UnsupportedTreeMarkingException("Unsupported tree marking: " + trimmedCode);
            }
            codes.add(trimmedCode);
        }
        return codes;
    }

    /** Kody znane katalogowi w kolejnosci katalogu (nieznane pominiete) - stabilny wynik w API. */
    public List<String> inCatalogOrder(Collection<String> codes) {
        return codes.stream().filter(this::isSupported).distinct()
                .sorted(Comparator.comparing(POSITION_BY_CODE::get)).toList();
    }
}
