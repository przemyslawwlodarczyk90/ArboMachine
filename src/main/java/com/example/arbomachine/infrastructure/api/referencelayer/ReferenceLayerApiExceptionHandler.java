package com.example.arbomachine.infrastructure.api.referencelayer;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.referencelayer.exception.ReferenceLayerFileRejectedException;
import com.example.arbomachine.domain.referencelayer.exception.ReferenceLayerNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/** Bledy modulu warstw: cudzy projekt / warstwa -> 404; plik odrzucony -> 400 z kodem (polski komunikat we frontendzie). */
@ControllerAdvice(assignableTypes = ReferenceLayerController.class)
public class ReferenceLayerApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(ReferenceLayerNotFoundException.class)
    public ResponseEntity<String> handleLayerNotFound(ReferenceLayerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(ReferenceLayerFileRejectedException.class)
    public ResponseEntity<Map<String, String>> handleFileRejected(ReferenceLayerFileRejectedException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", exception.code());
        if (exception.detail() != null) body.put("detail", exception.detail());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
