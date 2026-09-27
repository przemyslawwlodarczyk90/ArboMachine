package com.example.arbomachine.domain.tree.exception;

/**
 * Drzewo nie istnieje ALBO nalezy do innego projektu niz ten z URL (HTTP 404). Oba przypadki celowo
 * wygladaja tak samo - zgodnie z polityka projektow (brak dostepu = 404, bez zdradzania istnienia danych).
 */
public class TreeNotFoundException extends RuntimeException {
    public TreeNotFoundException(String message) {
        super(message);
    }
}
