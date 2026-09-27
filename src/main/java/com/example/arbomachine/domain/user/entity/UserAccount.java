package com.example.arbomachine.domain.user.entity;

import com.example.arbomachine.domain.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/** Konto uzytkownika: dane profilu, hash hasla, rola oraz flagi statusu konta. */
@Entity
@Table(name = "user_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    /** Hash BCrypt - nigdy haslo jawnym tekstem. */
    private String passwordHash;

    private String email;
    private String phoneNumber;
    private String street;
    private String postalCode;
    private String city;

    /** true po kliknieciu linku z maila potwierdzajacego (albo gdy admin potwierdzi e-mail za uzytkownika). */
    private boolean emailConfirmed;

    /**
     * Ustawiane przez admina. Zablokowane konto nie moze sie zalogowac, zarejestrowac ponownie
     * ani zresetowac hasla. Celowo osobne od emailConfirmed - inaczej ponowna rejestracja
     * "odblokowalaby" konto.
     */
    private boolean lockedByAdmin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    @OneToMany(mappedBy = "userAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EmailConfirmationToken> emailConfirmationTokens;
}
