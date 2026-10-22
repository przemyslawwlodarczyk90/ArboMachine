package com.example.arbomachine.domain.tree.entity;

import com.example.arbomachine.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Drzewo w projekcie - model V1: numer, gatunek (LAT/PL), podstawowe pomiary jako proste kolumny, notatka
 * i opcjonalny punkt lokalizacji (TreeLocation), a od etapu 17 takze opisowe pola formularza terenowego (stan drzewa,
 * zagrozenia - wolny tekst V1). To NIE jest docelowy model arborystyczny: bez strukturalnej oceny stanu/ryzyka,
 * zdjec; wiele pni i historia
 * pomiarow wymagalyby osobnych encji. Kolejne pola dochodza malymi, osobnymi zmianami.
 * Nie istnieje bez projektu. Nie ma wlasnego wlasciciela: wlasnosc wynika z Tree -> Project -> owner.
 * Relacja jednokierunkowa - Project nie trzyma kolekcji drzew (liste daje TreeRepository).
 */
@Entity
@Table(name = "tree", indexes = @Index(name = "idx_tree_project_id", columnList = "project_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tree_project_client_request", columnNames = {"project_id", "client_request_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tree {

    /** Techniczny limit numeru inwentaryzacyjnego = dlugosc kolumny inventory_number. */
    public static final int MAXIMUM_INVENTORY_NUMBER_LENGTH = 100;

    /** Techniczny limit nazwy lacinskiej = dlugosc kolumny species_latin. */
    public static final int MAXIMUM_SPECIES_LATIN_LENGTH = 255;

    /** Techniczny limit nazwy polskiej = dlugosc kolumny species_polish. */
    public static final int MAXIMUM_SPECIES_POLISH_LENGTH = 255;

    /** Zakres obwodu pnia w cm - ochrona przed literowkami (najwieksze drzewa swiata maja ok. 3000 cm). */
    public static final int MINIMUM_TRUNK_CIRCUMFERENCE_CM = 1;
    public static final int MAXIMUM_TRUNK_CIRCUMFERENCE_CM = 5000;

    /** Pomiary w metrach: numeric(5,2) - do 999.99 m z dokladnoscia do 1 cm. */
    public static final int MEASUREMENT_IN_METERS_PRECISION = 5;
    public static final int MEASUREMENT_IN_METERS_SCALE = 2;

    /** Gorna granica wysokosci drzewa w m (najwyzsze drzewa swiata maja ok. 116 m) - ochrona przed literowkami. */
    public static final String MAXIMUM_HEIGHT_M = "150";

    /** Gorna granica srednicy korony w m (figowce bengalskie przekraczaja 100 m) - ochrona przed literowkami. */
    public static final String MAXIMUM_CROWN_DIAMETER_M = "200";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tree_project"))
    private Project project;

    /**
     * Uzytkowy identyfikator drzewa nadawany przez arboryste (np. "1", "A-12", "PARK-15") - tekst, nie liczba.
     * Bez automatycznej numeracji i bez unikalnosci, dopoki zasady numeracji nie zostana potwierdzone.
     */
    @Column(name = "inventory_number", nullable = false, length = MAXIMUM_INVENTORY_NUMBER_LENGTH)
    private String inventoryNumber;

    /** Opcjonalna nazwa lacinska gatunku - zwykly tekst (bez slownika gatunkow na tym etapie). */
    @Column(name = "species_latin", length = MAXIMUM_SPECIES_LATIN_LENGTH)
    private String speciesLatin;

    /**
     * Opcjonalna polska nazwa gatunku (model V1) - zwykly tekst, niezalezny od nazwy lacinskiej.
     * Bez slownika i bez automatycznego wiazania PL <-> LAT; slownik Species to ewentualnie osobna zmiana modelu.
     */
    @Column(name = "species_polish", length = MAXIMUM_SPECIES_POLISH_LENGTH)
    private String speciesPolish;

    /**
     * Obwod pnia w centymetrach, liczba calkowita (model V1 - jeden pomiar, jeden pien, bez wysokosci pomiaru).
     * Jednostka jest w nazwie pola i kolumny. Wiele pni / historia pomiarow = przyszly TreeTrunk / Measurement.
     */
    @Column(name = "trunk_circumference_cm")
    private Integer trunkCircumferenceCm;

    /** Wysokosc drzewa w metrach, np. 18.50 (model V1 - bez metody i dokladnosci pomiaru). */
    @Column(name = "height_m", precision = MEASUREMENT_IN_METERS_PRECISION, scale = MEASUREMENT_IN_METERS_SCALE)
    private BigDecimal heightM;

    /**
     * Srednica korony w metrach (model V1 - jeden wymiar). Jesli potrzebne beda dwie osie (N-S, E-W),
     * dojdzie to osobna zmiana modelu.
     */
    @Column(name = "crown_diameter_m", precision = MEASUREMENT_IN_METERS_PRECISION, scale = MEASUREMENT_IN_METERS_SCALE)
    private BigDecimal crownDiameterM;

    /** Ogolna notatka pierwszego pionu - NIE docelowe miejsce na stan zdrowotny, zagrozenia czy zalecenia. */
    @Column(columnDefinition = "text")
    private String notes;

    /**
     * Opis stanu drzewa (zdrowotnego/statycznego) - V1: wolny tekst formularza terenowego. Swiadomie bez skali,
     * enumow i VTA, dopoki arborysta nie potwierdzi sposobu oceny; latwy do zamiany na model strukturalny.
     */
    @Column(name = "condition_notes", columnDefinition = "text")
    private String conditionNotes;

    /** Zagrozenia stwarzane przez drzewo - V1: wolny tekst, bez poziomow ryzyka (docs/DO_DOPRECYZOWANIA.md). */
    @Column(name = "hazard_notes", columnDefinition = "text")
    private String hazardNotes;

    /**
     * Opcjonalny punkt drzewa (kolumny latitude/longitude, obie null = brak lokalizacji). Nie jest polem
     * edytowalnym formularza karty drzewa - zmienia sie osobna operacja, bo docelowo punkt bedzie pochodzil
     * z mapy albo GNSS, a nie z edycji danych opisowych.
     */
    @Embedded
    private TreeLocation location;

    /**
     * Symbol na mapie projektu - kod z MapSymbolCatalog (np. RED_CROSS), null = neutralny wyglad. To dane prezentacji
     * mapy, a nie dane arborystyczne; nie jest czescia formularza edycji i zmienia sie osobna operacja (.../map-symbol).
     */
    @Column(name = "map_symbol_code", length = 64)
    private String mapSymbolCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Etap 37: wersja danych drzewa (pol formularza) - rosnie przy kazdej zmianie tych pol, NIE przy lokalizacji ani
     * symbolu. Zapis formularza z nieaktualna wersja (inne urzadzenie zmienilo dane w miedzyczasie) -> 409 zamiast
     * cichego nadpisania. Wlasny licznik zamiast @Version: przesuniecie punktu w terenie nie unieważnia formularza.
     */
    @Column(name = "data_version", nullable = false, columnDefinition = "bigint default 0 not null")
    private long dataVersion;

    /**
     * Etap 36: identyfikator utworzenia z urzadzenia (kolejka offline) - ponowienie po zerwanej odpowiedzi zwraca ten
     * rekord zamiast duplikatu. null = utworzony bez identyfikatora (online przed etapem 36, import).
     */
    @Column(name = "client_request_id", length = ClientRequestId.LENGTH, updatable = false)
    private String clientRequestId;

    /** Etap 36: tylko przy tworzeniu (kolumna nie jest aktualizowana). */
    public void assignClientRequestId(String normalizedClientRequestId) {
        this.clientRequestId = normalizedClientRequestId;
    }

    public static Tree createInProject(Project project, TreeEditableAttributes initialAttributes, Instant creationTime) {
        Tree tree = new Tree();
        tree.project = project;
        tree.applyEditableAttributes(initialAttributes);
        tree.createdAt = creationTime;
        tree.updatedAt = creationTime;
        return tree;
    }

    /** Aktualne wartosci pol edytowalnych - do porownania z wartosciami z requestu. */
    public TreeEditableAttributes editableAttributes() {
        return new TreeEditableAttributes(inventoryNumber, speciesLatin, speciesPolish, trunkCircumferenceCm, heightM,
                crownDiameterM, notes, conditionNotes, hazardNotes);
    }

    /** Zastepuje wszystkie pola edytowalne i przesuwa updatedAt. Projekt i createdAt nigdy sie tu nie zmieniaja. */
    public void replaceEditableAttributes(TreeEditableAttributes newAttributes, Instant updateTime) {
        applyEditableAttributes(newAttributes);
        this.updatedAt = updateTime;
        this.dataVersion++;
    }

    /** Ustawia albo zastepuje lokalizacje drzewa i przesuwa updatedAt. */
    public void placeAt(TreeLocation newLocation, Instant updateTime) {
        this.location = Objects.requireNonNull(newLocation, "Use clearLocation to remove the location");
        this.updatedAt = updateTime;
    }

    /** Drzewo wraca do stanu "lokalizacja nie ustalona" i przesuwa updatedAt. */
    public void clearLocation(Instant updateTime) {
        this.location = null;
        this.updatedAt = updateTime;
    }

    /** Ustawia albo zmienia symbol na mapie (kod juz sprawdzony w MapSymbolCatalog) i przesuwa updatedAt. */
    public void assignMapSymbol(String symbolCode, Instant updateTime) {
        this.mapSymbolCode = Objects.requireNonNull(symbolCode, "Use clearMapSymbol to remove the map symbol");
        this.updatedAt = updateTime;
    }

    /** Powrot do neutralnego wygladu na mapie i przesuniecie updatedAt. */
    public void clearMapSymbol(Instant updateTime) {
        this.mapSymbolCode = null;
        this.updatedAt = updateTime;
    }

    private void applyEditableAttributes(TreeEditableAttributes attributes) {
        this.inventoryNumber = attributes.inventoryNumber();
        this.speciesLatin = attributes.speciesLatin();
        this.speciesPolish = attributes.speciesPolish();
        this.trunkCircumferenceCm = attributes.trunkCircumferenceCm();
        this.heightM = attributes.heightM();
        this.crownDiameterM = attributes.crownDiameterM();
        this.notes = attributes.notes();
        this.conditionNotes = attributes.conditionNotes();
        this.hazardNotes = attributes.hazardNotes();
    }
}
