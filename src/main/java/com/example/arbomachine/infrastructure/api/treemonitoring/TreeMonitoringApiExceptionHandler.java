package com.example.arbomachine.infrastructure.api.treemonitoring;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import com.example.arbomachine.domain.treemonitoring.exception.TreeMonitoringRecordNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Bledy modulu monitoringu. Cudzy/nieistniejacy projekt, drzewo spoza projektu i wpis spoza drzewa daja to samo 404.
 * Bledy walidacji obsluguje wspolny ApiExceptionHandler.
 */
@ControllerAdvice(assignableTypes = {TreeMonitoringController.class, ProjectTreeMonitoringController.class})
public class TreeMonitoringApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeNotFoundException.class)
    public ResponseEntity<String> handleTreeNotFound(TreeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeMonitoringRecordNotFoundException.class)
    public ResponseEntity<String> handleRecordNotFound(TreeMonitoringRecordNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
