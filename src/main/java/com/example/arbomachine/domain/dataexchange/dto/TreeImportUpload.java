package com.example.arbomachine.domain.dataexchange.dto;

/** Plik importu z requestu (bez MultipartFile w domenie) - nazwa i tresc sa niezaufane. */
public record TreeImportUpload(String originalFileName, byte[] content) {}
