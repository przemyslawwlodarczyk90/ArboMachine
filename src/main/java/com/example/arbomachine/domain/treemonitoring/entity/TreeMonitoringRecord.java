package com.example.arbomachine.domain.treemonitoring.entity;

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
 * Wpis monitoringu (kontrola stanu) drzewa - etap 29. Jedno drzewo ma historie wielu kontroli. Zapis OCENY
 * ARBORYSTY: data kontroli, opcjonalna ocena stanu V1 i uwagi. System niczego nie wylicza.
 * <p>
 * Bez wlasnego wlasciciela i bez project_id: wlasnosc wynika z wpis -> Tree -> Project -> owner (jak TreeWork).
 * Zdjecie z kontroli - odlozone (zdjecia drzewa sa w module treephoto; DO_DOPRECYZOWANIA).
 */
@Entity
@Table(name = "tree_monitoring_record",
        indexes = @Index(name = "idx_tree_monitoring_record_tree_date", columnList = "tree_id, inspection_date"),
        uniqueConstraints = @UniqueConstraint(name = "uk_tree_monitoring_tree_client_request",
                columnNames = {"tree_id", "client_request_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TreeMonitoringRecord {

    /** Limit uwag - ochrona przed przypadkowo wklejonym ogromnym tekstem, nie regula domenowa. */
    public static final int MAXIMUM_NOTES_LENGTH = 5000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tree_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tree_monitoring_record_tree"))
    private Tree tree;

    /** Data kontroli (bez czasu); bez ograniczen "nie w przyszlosci / nie w przeszlosci". */
    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 32)
    private TreeHealthStatus healthStatus;

    @Column(columnDefinition = "text")
    private String notes;

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

    /** Komplet pol wpisu ustawianych formularzem (znormalizowany; data nigdy null). */
    public record Fields(LocalDate inspectionDate, TreeHealthStatus healthStatus, String notes) {
        public Fields {
            Objects.requireNonNull(inspectionDate, "inspectionDate");
        }
    }

    public static TreeMonitoringRecord createForTree(Tree tree, Fields fields, Instant creationTime) {
        TreeMonitoringRecord record = new TreeMonitoringRecord();
        record.tree = tree;
        record.apply(fields);
        record.createdAt = creationTime;
        record.updatedAt = creationTime;
        return record;
    }

    public Fields fields() {
        return new Fields(inspectionDate, healthStatus, notes);
    }

    /** Edycja bez zmian nie przesuwa updatedAt. */
    public boolean differsFrom(Fields otherFields) {
        return !fields().equals(otherFields);
    }

    /** Zastepuje komplet pol. Drzewo i createdAt nigdy sie tu nie zmieniaja. */
    public void replaceFields(Fields newFields, Instant updateTime) {
        apply(newFields);
        this.updatedAt = updateTime;
    }

    private void apply(Fields fields) {
        this.inspectionDate = fields.inspectionDate();
        this.healthStatus = fields.healthStatus();
        this.notes = fields.notes();
    }
}
