package com.example.arbomachine.infrastructure.api.tree;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bledy modulu drzew - dotyczy tylko TreeController (jak ProjectApiExceptionHandler dla projektow).
 * Bledy walidacji obsluguje wspolny ApiExceptionHandler.
 */
@ControllerAdvice(assignableTypes = TreeController.class)
public class TreeApiExceptionHandler {

    /** Cudzy albo nieistniejacy projekt z URL - ta sama polityka co w API projektow. */
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeNotFoundException.class)
    public ResponseEntity<String> handleTreeNotFound(TreeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
