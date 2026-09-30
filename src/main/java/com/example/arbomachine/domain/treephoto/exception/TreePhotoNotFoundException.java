package com.example.arbomachine.domain.treephoto.exception;

/**
 * Zdjecie nie istnieje ALBO nalezy do innego drzewa niz to z URL (HTTP 404). Oba przypadki celowo wygladaja
 * tak samo - zgodnie z polityka projektow i drzew (brak dostepu = 404, bez zdradzania istnienia danych).
 */
public class TreePhotoNotFoundException extends RuntimeException {
    public TreePhotoNotFoundException(String message) {
        super(message);
    }
}
