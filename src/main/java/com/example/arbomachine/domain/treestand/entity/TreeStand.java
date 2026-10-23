package com.example.arbomachine.domain.treestand.entity;

import com.example.arbomachine.domain.project.entity.Project;
import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Grupa drzew (etap 29) - obszar (wielokat WGS 84) w projekcie, np. szpaler, zadrzewienie, skupisko krzewow,
 * z przypisanymi drzewami (wiele do wielu: drzewo moze byc w kilku grupach). Geometria jako tekst GeoJSON "Polygon"
 * (V1 bez PostGIS); pole powierzchni liczone przy zapisie. Wlasnosc: Project -> owner.
 */
@Entity
@Table(name = "tree_stand", indexes = @Index(name = "idx_tree_stand_project_id", columnList = "project_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeStand {

    public static final int MAXIMUM_NAME_LENGTH = 255;
    public static final int MAXIMUM_NOTES_LENGTH = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tree_stand_project"))
    private Project project;

    @Column(nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String name;

    @Column(columnDefinition = "text")
    private String notes;

    /** GeoJSON "Polygon" w WGS 84 (zwalidowany przez StandPolygon). */
    @Column(name = "geometry_geojson", nullable = false, columnDefinition = "text")
    private String geometryGeoJson;

    @Column(name = "area_square_meters", precision = 14, scale = 2)
    private BigDecimal areaSquareMeters;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TreeStandSource source;

    @ManyToMany
    @JoinTable(name = "tree_stand_tree",
            joinColumns = @JoinColumn(name = "tree_stand_id", foreignKey = @ForeignKey(name = "fk_tree_stand_tree_stand")),
            inverseJoinColumns = @JoinColumn(name = "tree_id", foreignKey = @ForeignKey(name = "fk_tree_stand_tree_tree")))
    private Set<Tree> trees = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static TreeStand create(Project project, String name, String notes, String geometryGeoJson,
                                   BigDecimal areaSquareMeters, TreeStandSource source, Set<Tree> trees, Instant creationTime) {
        TreeStand stand = new TreeStand();
        stand.project = project;
        stand.source = source;
        stand.createdAt = creationTime;
        stand.replace(name, notes, geometryGeoJson, areaSquareMeters, trees, creationTime);
        return stand;
    }

    /** Zastepuje nazwe, uwagi, geometrie i przypisane drzewa (projekt, zrodlo i createdAt bez zmian). */
    public void replace(String newName, String newNotes, String newGeometryGeoJson, BigDecimal newAreaSquareMeters,
                        Set<Tree> newTrees, Instant updateTime) {
        this.name = newName;
        this.notes = newNotes;
        this.geometryGeoJson = newGeometryGeoJson;
        this.areaSquareMeters = newAreaSquareMeters;
        this.trees.clear();
        this.trees.addAll(newTrees);
        this.updatedAt = updateTime;
    }

    /** Dopisuje drzewa (np. lezace w obszarze); zwraca liczbe nowo dodanych. */
    public int addTrees(Set<Tree> treesToAdd, Instant updateTime) {
        int before = trees.size();
        trees.addAll(treesToAdd);
        if (trees.size() != before) this.updatedAt = updateTime;
        return trees.size() - before;
    }

    /** Etap 44: usuwane drzewo znika z grupy; updatedAt tylko, gdy drzewo bylo w grupie. */
    public void removeTree(Tree treeToRemove, Instant updateTime) {
        if (trees.remove(treeToRemove)) this.updatedAt = updateTime;
    }
}
