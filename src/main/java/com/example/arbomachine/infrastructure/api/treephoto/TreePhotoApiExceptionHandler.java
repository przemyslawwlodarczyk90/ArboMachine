package com.example.arbomachine.infrastructure.api.treephoto;

import com.example.arbomachine.domain.project.exception.ProjectNotFoundException;
import com.example.arbomachine.domain.tree.exception.TreeNotFoundException;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoFileMissingException;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoFileTooLargeException;
import com.example.arbomachine.domain.treephoto.exception.TreePhotoNotFoundException;
import com.example.arbomachine.domain.treephoto.exception.UnsupportedTreePhotoTypeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Bledy modulu zdjec - dotyczy tylko TreePhotoController. Cudzy/nieistniejacy projekt, drzewo spoza projektu
 * i zdjecie spoza drzewa daja to samo 404. Plik ponad limit multipart serwera (zanim zadanie dotrze do kontrolera)
 * obsluguje wspolny ApiExceptionHandler.
 */
@ControllerAdvice(assignableTypes = TreePhotoController.class)
public class TreePhotoApiExceptionHandler {

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<String> handleProjectNotFound(ProjectNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreeNotFoundException.class)
    public ResponseEntity<String> handleTreeNotFound(TreeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreePhotoNotFoundException.class)
    public ResponseEntity<String> handleTreePhotoNotFound(TreePhotoNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TreePhotoFileMissingException.class)
    public ResponseEntity<String> handleTreePhotoFileMissing(TreePhotoFileMissingException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    /** Zadanie multipart bez czesci "file". */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingFilePart(MissingServletRequestPartException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Photo file is mandatory");
    }

    @ExceptionHandler(TreePhotoFileTooLargeException.class)
    public ResponseEntity<String> handleTreePhotoFileTooLarge(TreePhotoFileTooLargeException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(exception.getMessage());
    }

    @ExceptionHandler(UnsupportedTreePhotoTypeException.class)
    public ResponseEntity<String> handleUnsupportedTreePhotoType(UnsupportedTreePhotoTypeException exception) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(exception.getMessage());
    }
}
