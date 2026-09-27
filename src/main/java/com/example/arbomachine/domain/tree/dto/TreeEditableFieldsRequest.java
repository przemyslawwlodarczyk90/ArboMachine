package com.example.arbomachine.domain.tree.dto;

/**
 * Pola drzewa wspolne dla CreateTreeRequest i UpdateTreeRequest. Oba requesty pozostaja osobnymi operacjami
 * (inne endpointy, mozliwe rozne zasady w przyszlosci) - interfejs pozwala tylko znormalizowac je jednym kodem.
 */
public interface TreeEditableFieldsRequest {

    String inventoryNumber();

    String speciesLatin();

    String notes();
}
