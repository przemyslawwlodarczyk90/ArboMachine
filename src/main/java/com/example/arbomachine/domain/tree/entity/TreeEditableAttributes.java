package com.example.arbomachine.domain.tree.entity;

/**
 * Komplet pol drzewa, ktore uzytkownik ustawia przy tworzeniu i edycji (bez id, projektu i dat).
 * Wartosci sa juz znormalizowane (przyciety tekst, puste pola opcjonalne jako null), wiec equals
 * odpowiada na pytanie "czy edycja cokolwiek zmienia". Nowe pole Tree V1 = nowy skladnik tego rekordu.
 */
public record TreeEditableAttributes(
        String inventoryNumber,
        String speciesLatin,
        String speciesPolish,
        Integer trunkCircumferenceCm,
        String notes
) {}
