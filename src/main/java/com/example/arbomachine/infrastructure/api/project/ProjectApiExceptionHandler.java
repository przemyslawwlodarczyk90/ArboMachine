package com.example.arbomachine.infrastructure.api.project;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bledy modulu projektow - osobny handler zamiast dopisywania do wspolnego ApiExceptionHandler,
 * zeby modul byl samodzielny. Bledy walidacji obsluguje dalej wspolny ApiExceptionHandler.
 */
@ControllerAdvice(assignableTypes = ProjectController.class)
public class ProjectApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
