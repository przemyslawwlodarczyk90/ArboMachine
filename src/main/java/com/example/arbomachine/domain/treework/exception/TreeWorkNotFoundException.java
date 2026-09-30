package com.example.arbomachine.domain.treework.exception;

/**
 * Praca nie istnieje ALBO nalezy do innego drzewa niz to z URL (HTTP 404). Oba przypadki celowo wygladaja
 * tak samo - zgodnie z polityka projektow i drzew (brak dostepu = 404, bez zdradzania istnienia danych).
 */
public class TreeWorkNotFoundException extends RuntimeException {
    public TreeWorkNotFoundException(String message) {
        super(message);
    }
}
