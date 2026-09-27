package com.example.arbomachine.domain.project.entity;

import com.example.arbomachine.domain.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_owner"))
    private UserAccount owner;

    @Column(nullable = false, length = MAXIMUM_NAME_LENGTH)
    private String name;

    /** Opcjonalny, dowolnie dlugi opis - w PostgreSQL kolumna typu text. */
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    private String description;

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
