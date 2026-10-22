package com.example.arbomachine.domain.treephoto.entity;

import com.example.arbomachine.domain.tree.entity.Tree;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Zdjecie drzewa - model V1: same metadane pliku. Binarna tresc NIE jest w bazie - lezy w PhotoStorage pod
 * kluczem storageKey (UUID nadany przez aplikacje; nazwa pliku od uzytkownika jest tylko opisem).
 * <p>
 * Celowo BEZ podpisu, kategorii, zdjecia glownego, kolejnosci, EXIF/GPS i adnotacji - te kwestie sa otwarte
 * (docs/DO_DOPRECYZOWANIA.md). Zdjecie nie ma wlasnego wlasciciela: wlasnosc wynika z
 * TreePhoto -> Tree -> Project -> owner. Relacja jednokierunkowa - Tree nie trzyma kolekcji zdjec.
 */
@Entity
@Table(name = "tree_photo", indexes = @Index(name = "idx_tree_photo_tree_id", columnList = "tree_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tree_photo_tree_client_upload",
                columnNames = {"tree_id", "client_upload_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreePhoto {

    /** Techniczny limit oryginalnej nazwy pliku = dlugosc kolumny original_file_name. */
    public static final int MAXIMUM_ORIGINAL_FILE_NAME_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tree_photo_tree"))
    private Tree tree;

    /** Nazwa pliku z urzadzenia uzytkownika - tylko do wyswietlenia, nigdy do budowania sciezki. */
    @Column(name = "original_file_name", nullable = false, length = MAXIMUM_ORIGINAL_FILE_NAME_LENGTH)
    private String originalFileName;

    /** Klucz pliku w PhotoStorage (UUID + rozszerzenie) - nie wychodzi poza backend. */
    @Column(name = "storage_key", nullable = false, unique = true, length = 64)
    private String storageKey;

    /** Typ wykryty z zawartosci pliku (image/jpeg, image/png, image/webp), a nie zadeklarowany przez klienta. */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Etap 33 (zdjecia offline): identyfikator nadany przez urzadzenie (UUID) - ponowne wyslanie tego samego zdjecia
     * (np. odpowiedz HTTP zginela po zapisie) zwraca istniejacy rekord zamiast drugiego. null = upload bez
     * identyfikatora (zdjecia sprzed etapu 33). Unikalny w ramach drzewa.
     */
    @Column(name = "client_upload_id", length = 36)
    private String clientUploadId;

    public static TreePhoto createForTree(Tree tree, String originalFileName, String storageKey, String contentType,
                                          long fileSizeBytes, Instant creationTime, String clientUploadId) {
        TreePhoto treePhoto = new TreePhoto();
        treePhoto.tree = tree;
        treePhoto.originalFileName = originalFileName;
        treePhoto.storageKey = storageKey;
        treePhoto.contentType = contentType;
        treePhoto.fileSizeBytes = fileSizeBytes;
        treePhoto.createdAt = creationTime;
        treePhoto.clientUploadId = clientUploadId;
        return treePhoto;
    }
}
