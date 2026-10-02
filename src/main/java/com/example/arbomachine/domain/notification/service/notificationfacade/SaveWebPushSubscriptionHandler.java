package com.example.arbomachine.domain.notification.service.notificationfacade;

import com.example.arbomachine.domain.notification.dto.WebPushSubscriptionRequest;
import com.example.arbomachine.domain.notification.entity.WebPushSubscription;
import com.example.arbomachine.domain.notification.exception.UnsupportedPushSubscriptionException;
import com.example.arbomachine.domain.notification.push.P256KeyCodec;
import com.example.arbomachine.domain.notification.push.WebPushEndpointPolicy;
import com.example.arbomachine.domain.user.entity.UserAccount;
import com.example.arbomachine.domain.user.service.useraccountfacade.FindUserAccountByEmailHandler;
import com.example.arbomachine.infrastructure.repository.WebPushSubscriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * "Otrzymuj powiadomienia push" na tym urzadzeniu: zapis (albo odnowienie) subskrypcji przegladarki dla zalogowanego
 * konta. Adres uslugi push i klucze sa sprawdzane (SSRF, poprawny punkt P-256, sekret 16 bajtow). Ta sama
 * przegladarka zgloszona z innego konta przechodzi na to konto (jedno urzadzenie = powiadomienia jednego konta).
 */
@Component
public class SaveWebPushSubscriptionHandler {
    private final FindUserAccountByEmailHandler findUserAccountByEmailHandler;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final WebPushEndpointPolicy webPushEndpointPolicy;

    public SaveWebPushSubscriptionHandler(FindUserAccountByEmailHandler findUserAccountByEmailHandler,
                                          WebPushSubscriptionRepository webPushSubscriptionRepository,
                                          WebPushEndpointPolicy webPushEndpointPolicy) {
        this.findUserAccountByEmailHandler = findUserAccountByEmailHandler;
        this.webPushSubscriptionRepository = webPushSubscriptionRepository;
        this.webPushEndpointPolicy = webPushEndpointPolicy;
    }

    @Transactional
    public void handle(String currentUserEmail, WebPushSubscriptionRequest request) {
        UserAccount currentUser = findUserAccountByEmailHandler.handle(currentUserEmail);
        String endpoint = request.endpoint().trim();
        webPushEndpointPolicy.validatedEndpoint(endpoint);
        String p256dh = request.keys().p256dh().trim();
        String auth = request.keys().auth().trim();
        validateKeys(p256dh, auth);
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        webPushSubscriptionRepository.findByEndpoint(endpoint).ifPresentOrElse(
                existing -> existing.renew(currentUser, p256dh, auth, now),
                () -> webPushSubscriptionRepository.save(WebPushSubscription.create(currentUser, endpoint, p256dh, auth, now)));
    }

    private static void validateKeys(String p256dh, String auth) {
        try {
            P256KeyCodec.decodePublicKey(P256KeyCodec.fromBase64Url(p256dh));
            if (P256KeyCodec.fromBase64Url(auth).length != 16) throw new IllegalArgumentException("auth");
        } catch (IllegalArgumentException invalidKey) {
            throw new UnsupportedPushSubscriptionException("Push subscription keys are invalid");
        }
    }
}
