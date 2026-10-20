package com.example.arbomachine.domain.treemarking.entity;

import com.example.arbomachine.domain.tree.entity.Tree;
import com.example.arbomachine.domain.treemarking.catalog.TreeMarkingCatalog;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Jedno oznaczenie przypisane drzewu (etap 31), np. "do usuniecia". Drzewo ma 0..n oznaczen, kazde kod najwyzej raz
 * (unikalnosc tree_id + code). Relacja jednokierunkowa - Tree nie zna oznaczen (osobny modul treemarking).
 * Wlasnosc: TreeMarking -> Tree -> Project -> owner.
 */
@Entity
@Table(name = "tree_marking",
        indexes = @Index(name = "idx_tree_marking_tree_id", columnList = "tree_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tree_marking_tree_code", columnNames = {"tree_id", "code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeMarking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tree_marking_tree"))
    private Tree tree;

    @Column(nullable = false, length = TreeMarkingCatalog.MAXIMUM_CODE_LENGTH)
    private String code;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static TreeMarking create(Tree tree, String code, Instant creationTime) {
        TreeMarking marking = new TreeMarking();
        marking.tree = tree;
        marking.code = code;
        marking.createdAt = creationTime;
        return marking;
    }
}
