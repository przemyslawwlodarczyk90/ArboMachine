package com.example.arbomachine.domain.treephoto.dto;

import org.springframework.core.io.InputStreamSource;

/**
 * Plik zdjecia przekazany z kontrolera do domeny - bez zaleznosci domeny od MultipartFile.
 * Wszystkie wartosci pochodza od klienta i sa niezaufane: nazwa sluzy tylko jako opis, typ jest weryfikowany
 * z zawartoscia pliku, rozmiar z limitem.
 *
 * @param originalFileName    nazwa pliku z urzadzenia (moze byc null albo zawierac sciezke)
 * @param declaredContentType typ zadeklarowany przez przegladarke (moze byc null)
 * @param fileSizeBytes       rozmiar pliku w bajtach
 * @param content             zrodlo tresci pliku (mozna otworzyc kilka razy)
 * @param clientUploadId      etap 33: UUID nadany przez urzadzenie (null = bez ochrony przed powtorzeniem)
 */
public record TreePhotoUpload(
        String originalFileName,
        String declaredContentType,
        long fileSizeBytes,
        InputStreamSource content,
        String clientUploadId
) {}
