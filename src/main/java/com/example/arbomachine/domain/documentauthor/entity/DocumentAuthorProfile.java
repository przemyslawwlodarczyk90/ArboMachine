package com.example.arbomachine.domain.documentauthor.entity;

import com.example.arbomachine.domain.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

/**
 * Stale elementy dokumentu (etap 42, 02 poz. 8.3) - dane autora opracowania i wykonawcy zapisane RAZ w profilu konta,
 * a nie w kazdym projekcie. Osobna encja 1:1 z UserAccount (osobny modul), zeby konto nie znalo dokumentow.
 * <p>
 * V1: wolny tekst bez struktury (imie i nazwisko, kwalifikacje, firma, dane firmy, opis metod i technik). Czy dane
 * wykonawcy maja byc per projekt (kilku autorow, podwykonawcy) - do doprecyzowania z klientem.
 */
@Entity
@Table(name = "document_author_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DocumentAuthorProfile {

    public static final int MAXIMUM_SHORT_TEXT_LENGTH = 255;
    public static final int MAXIMUM_DETAILS_LENGTH = 2000;
    public static final int MAXIMUM_METHODS_DESCRIPTION_LENGTH = 10000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_document_author_profile_user_account"))
    private UserAccount userAccount;

    @Column(name = "author_full_name", length = MAXIMUM_SHORT_TEXT_LENGTH)
    private String authorFullName;

    @Column(columnDefinition = "text")
    private String qualifications;

    @Column(name = "company_name", length = MAXIMUM_SHORT_TEXT_LENGTH)
    private String companyName;

    @Column(name = "company_details", columnDefinition = "text")
    private String companyDetails;

    @Column(name = "methods_description", columnDefinition = "text")
    private String methodsDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Komplet danych profilu (znormalizowany: przyciety tekst, puste = null). */
    public record Details(String authorFullName, String qualifications, String companyName, String companyDetails,
                          String methodsDescription) {

        public boolean isEmpty() {
            return authorFullName == null && qualifications == null && companyName == null && companyDetails == null
                    && methodsDescription == null;
        }
    }

    public static DocumentAuthorProfile createFor(UserAccount userAccount, Details details, Instant creationTime) {
        DocumentAuthorProfile profile = new DocumentAuthorProfile();
        profile.userAccount = userAccount;
        profile.apply(details);
        profile.createdAt = creationTime;
        profile.updatedAt = creationTime;
        return profile;
    }

    public Details details() {
        return new Details(authorFullName, qualifications, companyName, companyDetails, methodsDescription);
    }

    /** Zastepuje caly profil; updatedAt przesuwa sie tylko przy rzeczywistej zmianie. */
    public void replaceDetails(Details newDetails, Instant updateTime) {
        if (Objects.equals(details(), newDetails)) return;
        apply(newDetails);
        this.updatedAt = updateTime;
    }

    private void apply(Details details) {
        this.authorFullName = details.authorFullName();
        this.qualifications = details.qualifications();
        this.companyName = details.companyName();
        this.companyDetails = details.companyDetails();
        this.methodsDescription = details.methodsDescription();
    }
}
