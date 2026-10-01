package com.example.arbomachine.domain.projectdocument.generator;

/** Gotowy plik dokumentu: tresc, typ MIME i bezpieczna nazwa pliku do Content-Disposition (bez danych od uzytkownika). */
public record GeneratedDocument(byte[] content, String contentType, String fileName) {}
