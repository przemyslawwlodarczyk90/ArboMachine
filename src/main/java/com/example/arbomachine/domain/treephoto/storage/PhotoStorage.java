package com.example.arbomachine.domain.treephoto.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Mala, techniczna abstrakcja przechowywania plikow zdjec: zapis, odczyt, usuniecie po kluczu. Domena zna tylko
 * klucz (generowany przez nia, nigdy nazwa pliku od uzytkownika) - nie zna sciezek ani systemu plikow.
 * V1: FileSystemPhotoStorage; pozniej mozna podmienic na storage obiektowy (S3/MinIO) bez zmian w handlerach.
 * Celowo bez "frameworka storage" - tylko to, czego potrzebuja zdjecia drzew.
 */
public interface PhotoStorage {

    /** Zapisuje nowy plik pod kluczem; nigdy nie nadpisuje istniejacego (klucz juz zajety -> PhotoStorageException). */
    void store(String storageKey, InputStream photoContent);

    /** Plik pod kluczem; brak pliku -> PhotoStorageException. */
    Resource load(String storageKey);

    /** Usuwa plik pod kluczem; brak pliku nie jest bledem. */
    void delete(String storageKey);
}
