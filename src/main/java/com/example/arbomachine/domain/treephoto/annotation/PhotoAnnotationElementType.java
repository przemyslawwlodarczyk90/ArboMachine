package com.example.arbomachine.domain.treephoto.annotation;

/**
 * Rodzaje elementow adnotacji V1. LINE i ARROW maja dokladnie 2 punkty, TEXT - 1 punkt (lewy gorny rog tekstu),
 * FREEHAND - od 2 punktow (limit techniczny w PhotoAnnotationLimits). Bez wielokatow, prostokatow i krzywych.
 */
public enum PhotoAnnotationElementType {
    LINE,
    ARROW,
    FREEHAND,
    TEXT
}
