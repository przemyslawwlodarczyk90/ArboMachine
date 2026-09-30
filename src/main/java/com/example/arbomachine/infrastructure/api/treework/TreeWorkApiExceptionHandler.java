package com.example.arbomachine.infrastructure.api.treework;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import com.example.arbomachine.domain.treework.exception.TreeWorkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bledy modulu prac - dotyczy tylko TreeWorkController. Cudzy/nieistniejacy projekt, drzewo spoza projektu
 * i praca spoza drzewa daja to samo 404 (API nie zdradza istnienia cudzych danych).
 * Bledy walidacji obsluguje wspolny ApiExceptionHandler.
 */
@ControllerAdvice(assignableTypes = TreeWorkController.class)
public class TreeWorkApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeNotFoundException.class)
    public ResponseEntity<String> handleTreeNotFound(TreeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeWorkNotFoundException.class)
    public ResponseEntity<String> handleTreeWorkNotFound(TreeWorkNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
