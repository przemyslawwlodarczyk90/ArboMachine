package com.example.arbomachine.domain.notification.push;

import com.example.arbomachine.domain.notification.exception.UnsupportedPushSubscriptionException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Serwer wysyla zadania POST pod adres podany przez przegladarke - bez kontroli bylby to SSRF (atak na siec
 * wewnetrzna serwera). Dopuszczamy tylko HTTPS do znanych uslug push (lista w app.push.allowed-endpoint-hosts:
 * host rowny albo konczacy sie ".host"), bez portu i bez danych logowania w adresie.
 */
@Component
public class WebPushEndpointPolicy {
    private final WebPushSettings webPushSettings;

    public WebPushEndpointPolicy(WebPushSettings webPushSettings) {
        this.webPushSettings = webPushSettings;
    }

    /** Rzuca UnsupportedPushSubscriptionException, jesli pod ten adres nie wolno wysylac. */
    public URI validatedEndpoint(String endpoint) {
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException | NullPointerException invalidUri) {
            throw new UnsupportedPushSubscriptionException("Push endpoint is not a valid URL");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        if (webPushSettings.localTestEndpointsAllowed() && "http".equals(uri.getScheme())
                && ("localhost".equals(host) || "127.0.0.1".equals(host))) {
            return uri;
        }
        if (!"https".equals(uri.getScheme()) || uri.getRawUserInfo() != null || uri.getPort() != -1 || host.isEmpty()) {
            throw new UnsupportedPushSubscriptionException("Push endpoint must be an https URL of a push service");
        }
        boolean knownPushService = webPushSettings.allowedEndpointHosts().stream()
                .anyMatch(allowedHost -> host.equals(allowedHost) || host.endsWith("." + allowedHost));
        if (!knownPushService) throw new UnsupportedPushSubscriptionException("Push service is not supported");
        return uri;
    }
}
