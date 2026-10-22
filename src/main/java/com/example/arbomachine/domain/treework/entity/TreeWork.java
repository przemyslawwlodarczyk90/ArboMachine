package com.example.arbomachine.domain.treework.entity;

import com.example.arbomachine.domain.tree.entity.ClientRequestId;
import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "tree_work", indexes = @Index(name = "idx_tree_work_tree_id", columnList = "tree_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tree_work_tree_client_request", columnNames = {"tree_id", "client_request_id"}))
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

    /**
     * Gospodarka drzewostanem (etap 28): status, priorytet organizacyjny i terminy - wszystko opcjonalne poza statusem.
     * Kolumna status jest nullowa tylko dla prac sprzed etapu 28 (getStatus() zwraca wtedy RECOMMENDED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private TreeWorkStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 32)
    private TreeWorkPriority priority;

    /** Planowany termin - bez ograniczenia "nie wczesniej niz dzis" (dokumentacja moze dotyczyc przeszlosci). */
    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    /** Komplet pol pracy ustawianych formularzem (znormalizowany; status nigdy null). */
    public record Fields(String name, String description, TreeWorkStatus status, TreeWorkPriority priority,
                         LocalDate plannedDate, LocalDate completedDate) {
        public Fields {
            Objects.requireNonNull(status, "status");
        }
    }

    public static TreeWork createForTree(Tree tree, Fields fields, Instant creationTime) {
        TreeWork treeWork = new TreeWork();
        treeWork.tree = tree;
        treeWork.apply(fields);
        treeWork.createdAt = creationTime;
        treeWork.updatedAt = creationTime;
        return treeWork;
    }

    /** Status; prace sprzed etapu 28 (null w bazie) traktujemy jako zalecenie. */
    public TreeWorkStatus getStatus() {
        return status == null ? TreeWorkStatus.RECOMMENDED : status;
    }

    public Fields fields() {
        return new Fields(name, description, getStatus(), priority, plannedDate, completedDate);
    }

    /** Czy podane (znormalizowane) wartosci roznia sie od zapisanych - edycja bez zmian nie przesuwa updatedAt. */
    public boolean differsFrom(Fields otherFields) {
        return !fields().equals(otherFields);
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

    /** Zastepuje komplet pol (nazwa, opis, status, priorytet, terminy). Drzewo i createdAt nigdy sie tu nie zmieniaja. */
    public void replaceFields(Fields newFields, Instant updateTime) {
        apply(newFields);
        this.updatedAt = updateTime;
    }

    private void apply(Fields fields) {
        this.name = fields.name();
        this.description = fields.description();
        this.status = fields.status();
        this.priority = fields.priority();
        this.plannedDate = fields.plannedDate();
        this.completedDate = fields.completedDate();
    }
}
