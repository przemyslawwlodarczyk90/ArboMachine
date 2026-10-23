package com.example.arbomachine.domain.project.entity;

import com.example.arbomachine.domain.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Projekt - nadrzedna encja pracy arborysty. Na tym etapie nalezy wylacznie do uzytkownika,
 * ktory go utworzyl (brak wspoldzielenia).
 * <p>
 * Relacja jest jednokierunkowa (Project -> UserAccount): konto nie trzyma listy projektow,
 * a zapis projektu niczego nie kaskaduje na konto.
 */
@Entity
@Table(name = "project", indexes = @Index(name = "idx_project_owner_id", columnList = "owner_id"))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project {

    /** Maksymalna dlugosc nazwy = dlugosc kolumny name (domyslne varchar(255)). */
    public static final int MAXIMUM_NAME_LENGTH = 255;
    public static final int MAXIMUM_CLIENT_NAME_LENGTH = 255;
    public static final int MAXIMUM_SITE_ADDRESS_LENGTH = 500;
    public static final int MAXIMUM_CONTRACT_NUMBER_LENGTH = 100;
    public static final int MAXIMUM_DOCUMENT_PURPOSE_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_owner"))
    private UserAccount owner;

    @Column(nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String name;

    /**
     * Opcjonalny, dowolnie dlugi opis - kolumna typu text. Celowo zwykle mapowanie String (wiazane jako VARCHAR):
     * {@code @JdbcTypeCode(LONG32VARCHAR)} powodowal w PostgreSQL blad przy zapisie null ("Unable to bind parameter").
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * Dane zlecenia (etap 29) - opcjonalne, wolny tekst V1 (bez kartoteki klientow): zleceniodawca, adres obiektu
     * i data zlecenia. Docelowy model klienta - DO_DOPRECYZOWANIA.
     */
    @Column(name = "client_name", length = MAXIMUM_CLIENT_NAME_LENGTH)
    private String clientName;

    @Column(name = "site_address", length = MAXIMUM_SITE_ADDRESS_LENGTH)
    private String siteAddress;

    @Column(name = "order_date")
    private java.time.LocalDate orderDate;

    /** Etap 45 (02 poz. 8.3): numer umowy / zlecenia i cel opracowania - opcjonalne, wolny tekst V1, trafiaja do PDF. */
    @Column(name = "contract_number", length = MAXIMUM_CONTRACT_NUMBER_LENGTH)
    private String contractNumber;

    @Column(name = "document_purpose", columnDefinition = "text")
    private String documentPurpose;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Nowy projekt: tworzony i "ostatnio zmieniony" w tej samej chwili. */
    public static Project createOwnedBy(UserAccount owner, String name, String description, Instant creationTime) {
        Project project = new Project();
        project.owner = owner;
        project.name = name;
        project.description = description;
        project.createdAt = creationTime;
        project.updatedAt = creationTime;
        return project;
    }
}
