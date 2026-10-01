package com.example.arbomachine.infrastructure.api.dataexchange;

import com.example.arbomachine.domain.dataexchange.dto.TreeImportValidationResponse;
import com.example.arbomachine.domain.dataexchange.exception.TreeImportRejectedException;
import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Bledy importu/eksportu - ta sama polityka 404 co API projektow; odrzucony import -> 400 z wynikiem walidacji. */
@ControllerAdvice(assignableTypes = DataExchangeController.class)
public class DataExchangeApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(exception.getMessage());
    }

    @ExceptionHandler(TreeImportRejectedException.class)
    public ResponseEntity<TreeImportValidationResponse> handleRejectedImport(TreeImportRejectedException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.validationResult());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingFile(MissingServletRequestPartException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_PLAIN).body("Import file is mandatory");
    }
}
