package com.example.arbomachine.infrastructure.api.projectsummary;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Bledy podsumowania projektu - dotyczy tylko ProjectSummaryController (ta sama polityka 404 co API projektow). */
@ControllerAdvice(assignableTypes = ProjectSummaryController.class)
public class ProjectSummaryApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
