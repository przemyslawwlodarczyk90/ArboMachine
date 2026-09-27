package com.example.arbomachine.domain.project.exception;

/**
 * Projekt nie istnieje ALBO nalezy do innego uzytkownika (HTTP 404). Oba przypadki celowo wygladaja
 * tak samo - zgodnie z konwencja aplikacji (brak zasobu = 404) i zeby nie zdradzac istnienia cudzych projektow.
 */
public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(String message) {
        super(message);
    }
}
