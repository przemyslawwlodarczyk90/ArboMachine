package com.example.arbomachine.domain.projectsummary.dto;

/**
 * Podsumowanie jednego projektu - read-model liczony w bazie (COUNT), bez ladowania drzew, zdjec i prac.
 * Zawsze treeWithLocationCount + treeWithoutLocationCount == treeCount.
 *
 * @param treeCount                liczba drzew projektu
 * @param treeWithLocationCount    drzewa z zapisana lokalizacja
 * @param treeWithoutLocationCount drzewa bez lokalizacji
 * @param photoCount               liczba zdjec (TreePhoto), a nie liczba drzew ze zdjeciami
 * @param workCount                liczba zalecen/prac (TreeWork), a nie liczba drzew z pracami
 */
public record ProjectSummaryResponse(
        long treeCount,
        long treeWithLocationCount,
        long treeWithoutLocationCount,
        long photoCount,
        long workCount
) {}
