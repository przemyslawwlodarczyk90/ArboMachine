package com.example.arbomachine.domain.dataexchange.exception;

import com.example.arbomachine.domain.dataexchange.dto.TreeStandImportValidationResponse;

/** Import grup drzew odrzucony (bledy pliku albo obiektow) - HTTP 400 z wynikiem walidacji; nic nie zapisano. */
public class TreeStandImportRejectedException extends RuntimeException {
    private final TreeStandImportValidationResponse validation;

    public TreeStandImportRejectedException(TreeStandImportValidationResponse validation) {
        super("Tree stand import rejected");
        this.validation = validation;
    }

    public TreeStandImportValidationResponse validation() {
        return validation;
    }
}
