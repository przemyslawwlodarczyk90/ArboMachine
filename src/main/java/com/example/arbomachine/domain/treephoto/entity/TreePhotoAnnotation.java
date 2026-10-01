package com.example.arbomachine.domain.treephoto.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Aktualna adnotacja zdjecia (szkic ciec, strzalki, tekst) - V1: jedna na zdjecie, bez historii wersji.
 * Oryginalne TreePhoto NIGDY nie jest zmieniane. Adnotacja trzyma:
 * - documentJson - edytowalny, zwalidowany dokument wektorowy (wspolrzedne znormalizowane 0..1, schemaVersion),
 * - renderedStorageKey - osobny splaszczony PNG (zdjecie + rysunek) do dokumentacji, w PhotoStorage.
 * documentJson to wersjonowany dokument rysunkowy, a nie EAV - nie odpytujemy bazy o pojedyncze punkty.
 * Bez wlasciciela: TreePhotoAnnotation -> TreePhoto -> Tree -> Project -> owner.
 */
@Entity
@Table(name = "tree_photo_annotation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreePhotoAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_photo_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_tree_photo_annotation_photo"))
    private TreePhoto treePhoto;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "document_json", nullable = false, columnDefinition = "text")
    private String documentJson;

    /** Klucz splaszczonego PNG w PhotoStorage (UUID) - nie wychodzi poza backend. */
    @Column(name = "rendered_storage_key", nullable = false, length = 64)
    private String renderedStorageKey;

    @Column(name = "rendered_content_type", nullable = false, length = 100)
    private String renderedContentType;

    @Column(name = "rendered_file_size_bytes", nullable = false)
    private long renderedFileSizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static TreePhotoAnnotation createForPhoto(TreePhoto treePhoto, int schemaVersion, String documentJson,
                                                     String renderedStorageKey, String renderedContentType,
                                                     long renderedFileSizeBytes, Instant creationTime) {
        TreePhotoAnnotation annotation = new TreePhotoAnnotation();
        annotation.treePhoto = treePhoto;
        annotation.replaceDocument(schemaVersion, documentJson, renderedStorageKey, renderedContentType,
                renderedFileSizeBytes, creationTime);
        annotation.createdAt = creationTime;
        return annotation;
    }

    /** Zastepuje dokument i wskazanie na NOWY splaszczony PNG (stary plik usuwa handler po udanym zapisie). */
    public void replaceDocument(int newSchemaVersion, String newDocumentJson, String newRenderedStorageKey,
                                String newRenderedContentType, long newRenderedFileSizeBytes, Instant updateTime) {
        this.schemaVersion = newSchemaVersion;
        this.documentJson = newDocumentJson;
        this.renderedStorageKey = newRenderedStorageKey;
        this.renderedContentType = newRenderedContentType;
        this.renderedFileSizeBytes = newRenderedFileSizeBytes;
        this.updatedAt = updateTime;
    }
}
