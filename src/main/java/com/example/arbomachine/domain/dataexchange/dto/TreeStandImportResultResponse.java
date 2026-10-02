package com.example.arbomachine.domain.dataexchange.dto;

/** Wynik udanego importu grup drzew: nowe grupy i liczba przypisan drzew lezacych w obszarach. */
public record TreeStandImportResultResponse(int importedStandCount, int assignedTreeCount) {}
