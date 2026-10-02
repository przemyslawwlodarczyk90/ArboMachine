package com.example.arbomachine.domain.notification.push;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Konfiguracja Web Push (application.properties / arbomachine-secrets.properties):
 * <ul>
 *   <li>app.push.vapid-public-key / app.push.vapid-private-key - stale klucze VAPID (base64url); brak = klucze
 *       wygenerowane i zapisane w bazie przy pierwszym uzyciu,</li>
 *   <li>app.push.vapid-subject - kontakt administratora dla uslug push (mailto: albo https:),</li>
 *   <li>app.push.allowed-endpoint-hosts - hosty uslug push, do ktorych serwer moze wysylac (ochrona przed SSRF),</li>
 *   <li>app.push.allow-local-test-endpoints - TYLKO testy: zgoda na http://localhost (falszywa usluga push).</li>
 * </ul>
 */
@Component
public class WebPushSettings {

    private final String configuredPublicKey;
    private final String configuredPrivateKey;
    private final String subject;
    private final List<String> allowedEndpointHosts;
    private final boolean localTestEndpointsAllowed;

    public WebPushSettings(@Value("${app.push.vapid-public-key:}") String configuredPublicKey,
                           @Value("${app.push.vapid-private-key:}") String configuredPrivateKey,
                           @Value("${app.push.vapid-subject:mailto:powiadomienia@arbomachine.example}") String subject,
                           @Value("${app.push.allowed-endpoint-hosts:fcm.googleapis.com,push.services.mozilla.com,notify.windows.com,push.apple.com}")
                           String allowedEndpointHosts,
                           @Value("${app.push.allow-local-test-endpoints:false}") boolean localTestEndpointsAllowed) {
        this.configuredPublicKey = configuredPublicKey.trim();
        this.configuredPrivateKey = configuredPrivateKey.trim();
        this.subject = subject;
        this.allowedEndpointHosts = Arrays.stream(allowedEndpointHosts.split(",")).map(String::trim)
                .filter(host -> !host.isEmpty()).toList();
        this.localTestEndpointsAllowed = localTestEndpointsAllowed;
    }

    public boolean hasConfiguredKeys() {
        return !configuredPublicKey.isEmpty() && !configuredPrivateKey.isEmpty();
    }

    public String configuredPublicKey() {
        return configuredPublicKey;
    }

    public String configuredPrivateKey() {
        return configuredPrivateKey;
    }

    public String subject() {
        return subject;
    }

    public List<String> allowedEndpointHosts() {
        return allowedEndpointHosts;
    }

    public boolean localTestEndpointsAllowed() {
        return localTestEndpointsAllowed;
    }
}
