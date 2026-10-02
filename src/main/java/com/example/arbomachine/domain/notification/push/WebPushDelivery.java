package com.example.arbomachine.domain.notification.push;

import com.example.arbomachine.domain.notification.entity.WebPushSubscription;
import com.example.arbomachine.infrastructure.repository.WebPushSubscriptionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Wysylka powiadomienia na wszystkie urzadzenia (subskrypcje) uzytkownika: szyfrowanie RFC 8291, VAPID RFC 8292,
 * POST do uslugi push. 404/410 = subskrypcja wygasla (np. uzytkownik zablokowal powiadomienia) - usuwamy ja.
 * Inne bledy tylko logujemy: powiadomienie i tak jest zapisane na liscie w aplikacji.
 */
@Component
public class WebPushDelivery {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebPushDelivery.class);
    /** Jak dlugo usluga push ma przechowywac wiadomosc dla wylaczonego urzadzenia (1 doba). */
    private static final int TIME_TO_LIVE_SECONDS = 86_400;

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final WebPushPayloadEncryptor webPushPayloadEncryptor;
    private final VapidAuthorizationFactory vapidAuthorizationFactory;
    private final WebPushServerKeyProvider webPushServerKeyProvider;
    private final WebPushEndpointPolicy webPushEndpointPolicy;
    private final WebPushSettings webPushSettings;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public WebPushDelivery(WebPushSubscriptionRepository webPushSubscriptionRepository,
                           WebPushPayloadEncryptor webPushPayloadEncryptor,
                           VapidAuthorizationFactory vapidAuthorizationFactory,
                           WebPushServerKeyProvider webPushServerKeyProvider,
                           WebPushEndpointPolicy webPushEndpointPolicy, WebPushSettings webPushSettings,
                           ObjectMapper objectMapper) {
        this.webPushSubscriptionRepository = webPushSubscriptionRepository;
        this.webPushPayloadEncryptor = webPushPayloadEncryptor;
        this.vapidAuthorizationFactory = vapidAuthorizationFactory;
        this.webPushServerKeyProvider = webPushServerKeyProvider;
        this.webPushEndpointPolicy = webPushEndpointPolicy;
        this.webPushSettings = webPushSettings;
        this.objectMapper = objectMapper;
    }

    /** @return liczba urzadzen, ktore usluga push przyjela */
    public int deliverToAllDevicesOf(long userAccountId, WebPushMessage message) {
        List<WebPushSubscription> subscriptions = webPushSubscriptionRepository.findAllByUserAccountId(userAccountId);
        if (subscriptions.isEmpty()) return 0;
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException serializationError) {
            throw new IllegalStateException(serializationError);
        }
        int deliveredCount = 0;
        for (WebPushSubscription subscription : subscriptions) {
            if (deliverTo(subscription, payload)) deliveredCount++;
        }
        return deliveredCount;
    }

    private boolean deliverTo(WebPushSubscription subscription, byte[] payload) {
        try {
            // Adres sprawdzony przy zapisie subskrypcji; sprawdzamy ponownie (zmiana konfiguracji, stare wiersze).
            URI endpoint = webPushEndpointPolicy.validatedEndpoint(subscription.getEndpoint());
            byte[] body = webPushPayloadEncryptor.encrypt(payload, P256KeyCodec.fromBase64Url(subscription.getP256dhKey()),
                    P256KeyCodec.fromBase64Url(subscription.getAuthSecret()));
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("TTL", String.valueOf(TIME_TO_LIVE_SECONDS))
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("Urgency", "normal")
                    .header("Authorization", vapidAuthorizationFactory.authorizationHeaderFor(endpoint,
                            webPushServerKeyProvider.serverKeys(), webPushSettings.subject(), Instant.now()))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            int status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) {
                subscription.markDelivered(Instant.now());
                webPushSubscriptionRepository.save(subscription);
                return true;
            }
            if (status == 404 || status == 410) {
                webPushSubscriptionRepository.delete(subscription);
                LOGGER.info("Web Push subscription {} expired (HTTP {}) - removed", subscription.getId(), status);
            } else {
                LOGGER.warn("Web Push delivery to subscription {} failed: HTTP {}", subscription.getId(), status);
            }
        } catch (IOException | RuntimeException deliveryError) {
            LOGGER.warn("Web Push delivery to subscription {} failed: {}", subscription.getId(), deliveryError.toString());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
}
