package com.example.arbomachine.domain.dataexchange.dto;

/** Wynik udanego importu: liczba nowych drzew (wszystkie wiersze pliku) i liczba ostrzezen z walidacji. */
public record TreeImportResultResponse(int importedTreeCount, int warningCount) {}
