package com.example.arbomachine.infrastructure.api.projectdocument;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.projectdocument.generator.DocumentGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Bledy dokumentow projektu - ta sama polityka 404 co API projektow; blad renderowania -> 500 bez szczegolow. */
@ControllerAdvice(assignableTypes = ProjectDocumentController.class)
public class ProjectDocumentApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDocumentApiExceptionHandler.class);

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.TEXT_PLAIN).body(exception.getMessage());
    }

    @ExceptionHandler(DocumentGenerationException.class)
    public ResponseEntity<String> handleDocumentGenerationFailure(DocumentGenerationException exception) {
        LOGGER.error("Project document generation failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_PLAIN)
                .body("Document generation failed");
    }
}
