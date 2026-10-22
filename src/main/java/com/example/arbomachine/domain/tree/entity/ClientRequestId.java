package com.example.arbomachine.domain.tree.entity;

import java.util.Locale;
import java.util.UUID;

/**
 * Etap 36: identyfikator utworzenia nadany przez urzadzenie (UUID) - kolejka offline wysyla go przy tworzeniu drzewa,
 * pracy i kontroli stanu. Ponowne wyslanie po zerwanej odpowiedzi zwraca juz utworzony rekord zamiast duplikatu.
 * Wspolny dla modulow drzew, prac i monitoringu (wszystkie i tak zaleza od encji Tree).
 */
public final class ClientRequestId {

    public static final int LENGTH = 36;

    private ClientRequestId() {
    }

    /** null albo pusty = tworzenie bez ochrony przed powtorzeniem; inaczej UUID malymi literami; nie-UUID -> 400. */
    public static String normalizedOrNull(String clientRequestId) {
        if (clientRequestId == null || clientRequestId.isBlank()) return null;
        try {
            return UUID.fromString(clientRequestId.trim()).toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException notUuid) {
            throw new IllegalArgumentException("Client request id must be a UUID");
        }
    }
}
