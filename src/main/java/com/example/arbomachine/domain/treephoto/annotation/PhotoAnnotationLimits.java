package com.example.arbomachine.domain.treephoto.annotation;

/**
 * Techniczne limity dokumentu adnotacji V1 - ochrona przed ogromnymi dokumentami, nie reguly domenowe
 * (do weryfikacji w terenie, docs/DO_DOPRECYZOWANIA.md).
 */
public final class PhotoAnnotationLimits {

    /** Jedyna obslugiwana wersja schematu dokumentu adnotacji. */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final int MAXIMUM_ELEMENT_COUNT = 200;
    public static final int MAXIMUM_ELEMENT_ID_LENGTH = 64;
    public static final int MAXIMUM_TEXT_LENGTH = 500;
    public static final int MAXIMUM_FREEHAND_POINT_COUNT = 1000;
    public static final int MAXIMUM_TOTAL_POINT_COUNT = 20_000;

    private PhotoAnnotationLimits() {
    }
}
