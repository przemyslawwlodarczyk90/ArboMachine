package com.example.arbomachine.domain.treework.entity;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Zalecenie / praca przy drzewie - model V1: krotka nazwa wpisana przez arboryste (np. "Ciecia redukcyjne")
 * i opcjonalny opis. Jedno drzewo moze miec wiele prac.
 * <p>
 * Nazwa jest CELOWO zwyklym tekstem, a nie enumem typu pracy: slownik prac, kody, symbole mapowe, priorytet,
 * termin i status wykonania nie sa jeszcze potwierdzone (docs/DO_DOPRECYZOWANIA.md). Pozniejsza migracja moze
 * zamienic nazwe na odwolanie do slownika WorkType.
 * <p>
 * Nie ma wlasnego wlasciciela: wlasnosc wynika z TreeWork -> Tree -> Project -> owner.
 * Relacja jednokierunkowa - Tree nie trzyma kolekcji prac (liste daje TreeWorkRepository).
 */
@Entity
@Table(name = "tree_work", indexes = @Index(name = "idx_tree_work_tree_id", columnList = "tree_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeWork {

    /** Techniczny limit nazwy pracy = dlugosc kolumny name (jak nazwa projektu). */
    public static final int MAXIMUM_NAME_LENGTH = 255;

    /** Limit opisu - ochrona przed przypadkowo wklejonym ogromnym tekstem, nie regula domenowa. */
    public static final int MAXIMUM_DESCRIPTION_LENGTH = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tree_work_tree"))
    private Tree tree;

    @Column(nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

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

    public static TreeWork createForTree(Tree tree, String name, String description, Instant creationTime) {
        TreeWork treeWork = new TreeWork();
        treeWork.tree = tree;
        treeWork.name = name;
        treeWork.description = description;
        treeWork.createdAt = creationTime;
        treeWork.updatedAt = creationTime;
        return treeWork;
    }

    /** Czy podane (znormalizowane) wartosci roznia sie od zapisanych - edycja bez zmian nie przesuwa updatedAt. */
    public boolean differsFrom(String otherName, String otherDescription) {
        return !Objects.equals(name, otherName) || !Objects.equals(description, otherDescription);
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

    /** Zastepuje nazwe i opis. Drzewo i createdAt nigdy sie tu nie zmieniaja. */
    public void replaceNameAndDescription(String newName, String newDescription, Instant updateTime) {
        this.name = newName;
        this.description = newDescription;
        this.updatedAt = updateTime;
    }
}
