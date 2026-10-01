package com.example.arbomachine.domain.tree.entity;

/**
 * Skad pochodzi punkt drzewa (etap 25). Zapisywane jako tekst (varchar), nie enum bazy - latwo dopisac nowe zrodla
 * (np. zewnetrzny odbiornik GNSS, RTK). null w bazie = punkt zapisany przed etapem 25 (zrodlo nieznane).
 */
public enum TreeLocationSource {
    /** wspolrzedne wpisane recznie w formularzu */
    MANUAL,
    /** punkt wskazany na mapie */
    MAP,
    /** pomiar GNSS urzadzenia (przegladarkowe Geolocation API - GPS tabletu/telefonu) */
    DEVICE_GNSS,
    /** punkt z importu pliku (CSV / XLSX / GeoJSON) */
    IMPORT
}
