package com.example.arbomachine.domain.notification.entity;

import com.example.arbomachine.domain.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Powiadomienie uzytkownika (etap 29-30) - zapisywane ZAWSZE, niezaleznie od tego, czy push dotarl (lista
 * "Powiadomienia" w aplikacji). deduplicationKey - to samo zdarzenie nie tworzy drugiego powiadomienia
 * (np. jedno przypomnienie o pracach na dany dzien).
 */
@Entity
@Table(name = "user_notification",
        indexes = @Index(name = "idx_user_notification_user_created", columnList = "user_account_id, created_at"),
        uniqueConstraints = @UniqueConstraint(name = "uk_user_notification_deduplication",
                columnNames = {"user_account_id", "deduplication_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

    public static final int MAXIMUM_TITLE_LENGTH = 200;
    public static final int MAXIMUM_BODY_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_user_notification_user_account"))
    private UserAccount userAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserNotificationType type;

    @Column(nullable = false, length = MAXIMUM_TITLE_LENGTH)
    private String title;

    @Column(nullable = false, length = MAXIMUM_BODY_LENGTH)
    private String body;

    /** Trasa frontendu otwierana po kliknieciu (np. /projects/1/trees/2); zawsze sciezka wzgledna aplikacji. */
    @Column(name = "link_path", length = 255)
    private String linkPath;

    @Column(name = "deduplication_key", nullable = false, length = 128)
    private String deduplicationKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static UserNotification create(UserAccount userAccount, UserNotificationType type, String title, String body,
                                          String linkPath, String deduplicationKey, Instant creationTime) {
        UserNotification notification = new UserNotification();
        notification.userAccount = userAccount;
        notification.type = type;
        notification.title = truncate(title, MAXIMUM_TITLE_LENGTH);
        notification.body = truncate(body, MAXIMUM_BODY_LENGTH);
        notification.linkPath = linkPath;
        notification.deduplicationKey = deduplicationKey;
        notification.createdAt = creationTime;
        return notification;
    }

    private static String truncate(String text, int maximumLength) {
        return text.length() <= maximumLength ? text : text.substring(0, maximumLength - 1) + "…";
    }
}
