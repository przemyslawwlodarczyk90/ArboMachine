package com.example.arbomachine.domain.referencelayer.dto;

/** Plik warstwy z requestu (bez MultipartFile w domenie) - nazwa pliku, tresc i nazwa warstwy sa niezaufane. */
public record ReferenceLayerUpload(String originalFileName, byte[] content, String requestedName) {}
