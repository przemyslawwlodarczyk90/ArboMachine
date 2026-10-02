package com.example.arbomachine.domain.notification.entity;

import com.example.arbomachine.domain.user.entity.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Subskrypcja Web Push jednej przegladarki (urzadzenia) uzytkownika - wynik PushManager.subscribe(). Istnienie
 * subskrypcji = "Otrzymuj powiadomienia push" wlaczone na tym urzadzeniu. Endpoint jest unikalny: ta sama
 * przegladarka po zalogowaniu na inne konto przechodzi na to konto.
 */
@Entity
@Table(name = "web_push_subscription",
        indexes = @Index(name = "idx_web_push_subscription_user", columnList = "user_account_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_web_push_subscription_endpoint", columnNames = "endpoint"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebPushSubscription {

    public static final int MAXIMUM_ENDPOINT_LENGTH = 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_account_id", nullable = false, foreignKey = @ForeignKey(name = "fk_web_push_subscription_user_account"))
    private UserAccount userAccount;

    @Column(nullable = false, length = MAXIMUM_ENDPOINT_LENGTH)
    private String endpoint;

    /** Klucz publiczny przegladarki (p256dh), base64url. */
    @Column(name = "p256dh_key", nullable = false, length = 128)
    private String p256dhKey;

    /** Sekret uwierzytelniania (auth), base64url. */
    @Column(name = "auth_secret", nullable = false, length = 64)
    private String authSecret;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Ostatnie udane doreczenie (null = jeszcze zadnego). */
    @Column(name = "last_delivered_at")
    private Instant lastDeliveredAt;

    public static WebPushSubscription create(UserAccount userAccount, String endpoint, String p256dhKey, String authSecret,
                                             Instant creationTime) {
        WebPushSubscription subscription = new WebPushSubscription();
        subscription.endpoint = endpoint;
        subscription.createdAt = creationTime;
        subscription.renew(userAccount, p256dhKey, authSecret, creationTime);
        return subscription;
    }

    /** Ponowne zgloszenie tej samej przegladarki (nowe klucze albo inne konto). */
    public void renew(UserAccount newUserAccount, String newP256dhKey, String newAuthSecret, Instant updateTime) {
        this.userAccount = newUserAccount;
        this.p256dhKey = newP256dhKey;
        this.authSecret = newAuthSecret;
        this.updatedAt = updateTime;
    }

    public void markDelivered(Instant deliveryTime) {
        this.lastDeliveredAt = deliveryTime;
    }
}
