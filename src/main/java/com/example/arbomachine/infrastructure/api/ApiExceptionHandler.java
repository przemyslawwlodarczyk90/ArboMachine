package com.example.arbomachine.infrastructure.api;

import com.example.arbomachine.domain.user.exception.*;
import com.example.arbomachine.infrastructure.service.EmailSendingFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * Zamienia wyjatki domenowe na odpowiedzi HTTP. Bledy walidacji wracaja jako mapa
 * {"nazwaPola": "komunikat"}, pozostale jako sam tekst komunikatu (frontend tlumaczy je na przyjazne).
 */
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException validationException) {
        Map<String, String> messagesByFieldName = new HashMap<>();
        validationException.getBindingResult().getFieldErrors()
                .forEach(fieldError -> messagesByFieldName.put(fieldError.getField(), fieldError.getDefaultMessage()));
        return ResponseEntity.badRequest().body(messagesByFieldName);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<String> handleTokenNotFound(TokenNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<String> handleTokenExpired(TokenExpiredException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    @ExceptionHandler(UserAccountNotFoundException.class)
    public ResponseEntity<String> handleUserAccountNotFound(UserAccountNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exception.getMessage());
    }

    @ExceptionHandler(EmailNotConfirmedException.class)
    public ResponseEntity<String> handleEmailNotConfirmed(EmailNotConfirmedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<String> handleEmailAlreadyInUse(EmailAlreadyInUseException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    /** Inny modul (np. projekty) nie pozwala usunac konta, bo naleza do niego jego dane. */
    @ExceptionHandler(UserAccountDeletionBlockedException.class)
    public ResponseEntity<String> handleUserAccountDeletionBlocked(UserAccountDeletionBlockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getMessage());
    }

    /** Np. "Email cannot be changed" przy edycji profilu. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    @ExceptionHandler(AccountLockedByAdminException.class)
    public ResponseEntity<String> handleAccountLockedByAdmin(AccountLockedByAdminException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(AdminAccountModificationForbiddenException.class)
    public ResponseEntity<String> handleAdminAccountModificationForbidden(AdminAccountModificationForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    /** Nieczytelne body albo nieznana wartosc enuma, np. "accountType": "ADMIN" albo ?role=FOO. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<String> handleMalformedRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Malformed request");
    }

    @ExceptionHandler(EmailSendingFailedException.class)
    public ResponseEntity<String> handleEmailSendingFailed(EmailSendingFailedException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(exception.getMessage());
    }
}
