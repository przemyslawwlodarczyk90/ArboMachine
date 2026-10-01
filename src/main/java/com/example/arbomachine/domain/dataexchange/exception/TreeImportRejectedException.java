package com.example.arbomachine.domain.dataexchange.exception;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;

/** Import odrzucony, bo plik ma bledy (HTTP 400 z pelnym wynikiem walidacji) - nic nie zostalo zapisane. */
public class TreeImportRejectedException extends RuntimeException {
    private final TreeImportValidationResponse validationResult;

    public TreeImportRejectedException(TreeImportValidationResponse validationResult) {
        super("Tree import rejected");
        this.validationResult = validationResult;
    }

    public TreeImportValidationResponse validationResult() {
        return validationResult;
    }
}
