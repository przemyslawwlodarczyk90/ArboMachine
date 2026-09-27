package com.example.arbomachine.domain.tree.entity;

import com.example.arbomachine.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Drzewo w projekcie - celowo minimalny model pierwszego pionu (bez lokalizacji, pomiarow, oceny).
 * Nie istnieje bez projektu. Nie ma wlasnego wlasciciela: wlasnosc wynika z Tree -> Project -> owner.
 * Relacja jednokierunkowa - Project nie trzyma kolekcji drzew (liste daje TreeRepository).
 */
@Entity
@Table(name = "tree", indexes = @Index(name = "idx_tree_project_id", columnList = "project_id"))
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

    /** Ogolna notatka pierwszego pionu - NIE docelowe miejsce na stan zdrowotny, zagrozenia czy zalecenia. */
    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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
        return new TreeEditableAttributes(inventoryNumber, speciesLatin, speciesPolish, trunkCircumferenceCm, heightM, notes);
    }

    /** Zastepuje wszystkie pola edytowalne i przesuwa updatedAt. Projekt i createdAt nigdy sie tu nie zmieniaja. */
    public void replaceEditableAttributes(TreeEditableAttributes newAttributes, Instant updateTime) {
        applyEditableAttributes(newAttributes);
        this.updatedAt = updateTime;
    }

    private void applyEditableAttributes(TreeEditableAttributes attributes) {
        this.inventoryNumber = attributes.inventoryNumber();
        this.speciesLatin = attributes.speciesLatin();
        this.speciesPolish = attributes.speciesPolish();
        this.trunkCircumferenceCm = attributes.trunkCircumferenceCm();
        this.heightM = attributes.heightM();
        this.notes = attributes.notes();
    }
}
