package com.example.arbomachine.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Jednorazowy token z linku aktywacyjnego wysylanego po rejestracji. */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfirmationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne
    @JoinColumn(nullable = false, name = "user_account_id")
    private UserAccount userAccount;
}
