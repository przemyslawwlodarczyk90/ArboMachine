package com.example.arbomachine.domain.referencelayer.entity;

import com.example.arbomachine.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Warstwa referencyjna projektu (etap 31) - "podklad wektorowy z QGIS": plik GIS/CAD (GeoJSON, Shapefile ZIP,
 * GeoPackage, DXF) wgrany przez arboryste i pokazywany na mapie POD drzewami, tylko do podgladu (granice, dzialki,
 * drogi, sieci, wlasna inwentaryzacja). Geometria zapisana jako GeoJSON FeatureCollection w WGS 84 (V1 bez PostGIS,
 * bez edycji obiektow). Wlasnosc: Project -> owner.
 */
@Entity
@Table(name = "project_reference_layer", indexes = @Index(name = "idx_project_reference_layer_project_id", columnList = "project_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectReferenceLayer {

    public static final int MAXIMUM_NAME_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_reference_layer_project"))
    private Project project;

    @Column(nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String name;

    @Column(name = "source_file_name", length = MAXIMUM_NAME_LENGTH)
    private String sourceFileName;

    /** GEOJSON, SHAPEFILE, GEOPACKAGE, DXF */
    @Column(name = "source_format", nullable = false, length = 32)
    private String sourceFormat;

    /** Uklad pliku zrodlowego, np. "EPSG:2178" (dane i tak zapisane w WGS 84). */
    @Column(name = "source_crs", length = 32)
    private String sourceCrs;

    @Column(name = "feature_count", nullable = false)
    private int featureCount;

    /** GeoJSON FeatureCollection w WGS 84 - wspolrzedne zaokraglone do 7 miejsc (ok. 1 cm). */
    @Column(name = "geojson", nullable = false, columnDefinition = "text")
    private String geoJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static ProjectReferenceLayer create(Project project, String name, String sourceFileName, String sourceFormat,
                                               String sourceCrs, int featureCount, String geoJson, Instant creationTime) {
        ProjectReferenceLayer layer = new ProjectReferenceLayer();
        layer.project = project;
        layer.name = name;
        layer.sourceFileName = sourceFileName;
        layer.sourceFormat = sourceFormat;
        layer.sourceCrs = sourceCrs;
        layer.featureCount = featureCount;
        layer.geoJson = geoJson;
        layer.createdAt = creationTime;
        return layer;
    }
}
