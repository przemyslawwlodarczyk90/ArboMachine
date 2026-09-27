package com.example.arbomachine.domain.tree.entity;

import com.example.arbomachine.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Drzewo w projekcie - celowo minimalny model pierwszego pionu (bez lokalizacji, pomiarow, oceny).
 * Nie istnieje bez projektu. Nie ma wlasnego wlasciciela: wlasnosc wynika z Tree -> Project -> owner.
 * Relacja jednokierunkowa - Project nie trzyma kolekcji drzew (liste daje TreeRepository).
 */
@Entity
@Table(name = "tree", indexes = @Index(name = "idx_tree_project_id", columnList = "project_id"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tree {

    /** Techniczny limit numeru inwentaryzacyjnego = dlugosc kolumny inventory_number. */
    public static final int MAXIMUM_INVENTORY_NUMBER_LENGTH = 100;

    /** Techniczny limit nazwy lacinskiej = dlugosc kolumny species_latin. */
    public static final int MAXIMUM_SPECIES_LATIN_LENGTH = 255;

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

    /** Ogolna notatka pierwszego pionu - NIE docelowe miejsce na stan zdrowotny, zagrozenia czy zalecenia. */
    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static Tree createInProject(Project project, String inventoryNumber, String speciesLatin, String notes,
                                       Instant creationTime) {
        Tree tree = new Tree();
        tree.project = project;
        tree.inventoryNumber = inventoryNumber;
        tree.speciesLatin = speciesLatin;
        tree.notes = notes;
        tree.createdAt = creationTime;
        tree.updatedAt = creationTime;
        return tree;
    }
}
