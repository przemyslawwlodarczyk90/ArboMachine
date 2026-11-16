package com.example.arbomachine.domain.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Ustawienia trybu demo i rozpoznawanie konta demo.
 * <p>
 * Konto demo to zwykle konto arborysty z adresem z konfiguracji (domyslnie w domenie .invalid - nie da sie go
 * zarejestrowac ani dostac na niego maila). Zapis z tego konta blokuje zawsze DemoReadOnlyFilter - takze wtedy,
 * gdy tryb demo jest wylaczony (app.demo.enabled=false), a konto zostalo w bazie po wczesniejszym wlaczeniu.
 */
@Component
public class DemoAccountPolicy {

    public static final String DEFAULT_DEMO_ACCOUNT_EMAIL = "demo@arbomachine.invalid";

    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled = false;

    @Value("${app.demo.account-email:" + DEFAULT_DEMO_ACCOUNT_EMAIL + "}")
    private String demoAccountEmail = DEFAULT_DEMO_ACCOUNT_EMAIL;

    @Value("${app.demo.session-hours:4}")
    private long sessionHours = 4;

    public DemoAccountPolicy() {
    }

    /** Konstruktor dla testow jednostkowych (bez Springa). */
    public DemoAccountPolicy(boolean demoEnabled, String demoAccountEmail, long sessionHours) {
        this.demoEnabled = demoEnabled;
        this.demoAccountEmail = demoAccountEmail;
        this.sessionHours = sessionHours;
    }

    public boolean isDemoEnabled() {
        return demoEnabled;
    }

    public String demoAccountEmail() {
        return demoAccountEmail.trim().toLowerCase(Locale.ROOT);
    }

    public Duration sessionDuration() {
        return Duration.ofHours(Math.max(1, sessionHours));
    }

    public boolean isDemoAccountEmail(String email) {
        return email != null && email.trim().toLowerCase(Locale.ROOT).equals(demoAccountEmail());
    }
}
