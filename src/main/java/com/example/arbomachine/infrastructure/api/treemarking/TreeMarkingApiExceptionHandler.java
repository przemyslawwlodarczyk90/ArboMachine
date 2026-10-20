package com.example.arbomachine.infrastructure.api.treemarking;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import com.example.arbomachine.domain.treemarking.exception.UnsupportedTreeMarkingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Bledy modulu oznaczen: cudzy projekt / drzewo -> 404, kod spoza katalogu -> 400. */
@ControllerAdvice(assignableTypes = TreeMarkingController.class)
public class TreeMarkingApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeNotFoundException.class)
    public ResponseEntity<String> handleTreeNotFound(TreeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(UnsupportedTreeMarkingException.class)
    public ResponseEntity<String> handleUnsupportedMarking(UnsupportedTreeMarkingException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }
}
