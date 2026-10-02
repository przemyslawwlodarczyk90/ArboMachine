package com.example.arbomachine.infrastructure.api.treestand;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.treestand.exception.InvalidTreeStandException;
import com.example.arbomachine.domain.treestand.exception.TreeStandNotFoundException;
import com.example.arbomachine.domain.treestand.geometry.InvalidStandGeometryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Bledy modulu grup drzew: cudzy projekt / grupa -> 404; zla geometria albo drzewo spoza projektu -> 400. */
@ControllerAdvice(assignableTypes = TreeStandController.class)
public class TreeStandApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeStandNotFoundException.class)
    public ResponseEntity<String> handleStandNotFound(TreeStandNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler({InvalidStandGeometryException.class, InvalidTreeStandException.class})
    public ResponseEntity<String> handleInvalidStand(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
