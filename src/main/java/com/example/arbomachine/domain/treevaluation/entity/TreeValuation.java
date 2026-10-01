package com.example.arbomachine.domain.treevaluation.entity;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Waloryzacja drzewa (etap 27) - ZAPIS OCENY ARBORYSTY, a nie wynik algorytmu. Osobna encja 1:1 z Tree, bo
 * inwentaryzacja opisuje drzewo, a waloryzacja interpretuje jego wartosc (inne zrodlo, inny moment pracy).
 * <p>
 * V1: opcjonalna ogolna kategoria (TreeValuationCategory, null = nie oceniono) i opisowe wartosci skladowe -
 * wiekowa, przyrodnicza, ekologiczna, kulturowa, krajobrazowa - jako wolny tekst, bez skal, punktow, wag i kwot.
 * Metoda waloryzacji nie jest potwierdzona (docs/discovery/etap-27-waloryzacja.md).
 * Bez wlasnego wlasciciela: TreeValuation -> Tree -> Project -> owner. Brak historii wersji (createdAt/updatedAt).
 */
@Entity
@Table(name = "tree_valuation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeValuation {

    /** Techniczny limit kazdego opisu - ochrona przed wklejonym ogromnym tekstem (jak opis pracy), nie regula domenowa. */
    public static final int MAXIMUM_TEXT_LENGTH = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_tree_valuation_tree"))
    private Tree tree;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_category", length = 32)
    private TreeValuationCategory overallCategory;

    @Column(name = "age_value", columnDefinition = "text")
    private String ageValue;

    @Column(name = "natural_value", columnDefinition = "text")
    private String naturalValue;

    @Column(name = "ecological_value", columnDefinition = "text")
    private String ecologicalValue;

    @Column(name = "cultural_value", columnDefinition = "text")
    private String culturalValue;

    @Column(name = "landscape_value", columnDefinition = "text")
    private String landscapeValue;

    @Column(columnDefinition = "text")
    private String justification;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Komplet pol oceny (znormalizowany: przyciety tekst, puste = null). */
    public record Assessment(TreeValuationCategory overallCategory, String ageValue, String naturalValue,
                             String ecologicalValue, String culturalValue, String landscapeValue,
                             String justification, String notes) {

        public boolean isEmpty() {
            return overallCategory == null && ageValue == null && naturalValue == null && ecologicalValue == null
                    && culturalValue == null && landscapeValue == null && justification == null && notes == null;
        }
    }

    public static TreeValuation createForTree(Tree tree, Assessment assessment, Instant creationTime) {
        TreeValuation valuation = new TreeValuation();
        valuation.tree = tree;
        valuation.apply(assessment);
        valuation.createdAt = creationTime;
        valuation.updatedAt = creationTime;
        return valuation;
    }

    public Assessment assessment() {
        return new Assessment(overallCategory, ageValue, naturalValue, ecologicalValue, culturalValue, landscapeValue,
                justification, notes);
    }

    /** Zastepuje cala ocene; updatedAt przesuwa sie tylko przy rzeczywistej zmianie. */
    public void replaceAssessment(Assessment newAssessment, Instant updateTime) {
        if (Objects.equals(assessment(), newAssessment)) return;
        apply(newAssessment);
        this.updatedAt = updateTime;
    }

    private void apply(Assessment assessment) {
        this.overallCategory = assessment.overallCategory();
        this.ageValue = assessment.ageValue();
        this.naturalValue = assessment.naturalValue();
        this.ecologicalValue = assessment.ecologicalValue();
        this.culturalValue = assessment.culturalValue();
        this.landscapeValue = assessment.landscapeValue();
        this.justification = assessment.justification();
        this.notes = assessment.notes();
    }
}
