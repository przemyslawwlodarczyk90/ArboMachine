package com.example.arbomachine.domain.treephoto.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.nio.file.Path;

/**
 * Ustawienia zdjec drzew (prefiks app.tree-photos) - JEDYNE miejsce limitu rozmiaru pliku; z niego korzysta
 * walidacja uploadu i limit multipart serwera (TreePhotoUploadConfig).
 *
 * @param maxFileSize      maksymalny rozmiar jednego zdjecia; V1: 15 MB - rozsadny limit developerski
 *                         (zdjecie z tabletu/telefonu to zwykle 2-8 MB), do potwierdzenia (docs/DO_DOPRECYZOWANIA.md)
 * @param storageDirectory katalog plikow zdjec na dysku (storage V1); sciezka wzgledna liczona od katalogu roboczego
 */
@ConfigurationProperties("app.tree-photos")
public record TreePhotoProperties(
        @DefaultValue("15MB") DataSize maxFileSize,
        @DefaultValue("storage/tree-photos") Path storageDirectory
) {}
