package com.example.arbomachine.domain.tree.dto;

import java.math.BigDecimal;

/**
 * Pola drzewa wspolne dla CreateTreeRequest i UpdateTreeRequest. Oba requesty pozostaja osobnymi operacjami
 * (inne endpointy, mozliwe rozne zasady w przyszlosci) - interfejs pozwala tylko znormalizowac je jednym kodem.
 */
public interface TreeEditableFieldsRequest {

    String inventoryNumber();

    String speciesLatin();

    String speciesPolish();

    /** BigDecimal zamiast Integer: Jackson po cichu obcialby 123.7 do 123, a tak walidacja zwraca czytelny blad. */
    BigDecimal trunkCircumferenceCm();

    BigDecimal heightM();

    BigDecimal crownDiameterM();

    String notes();

    String conditionNotes();

    String hazardNotes();
}
