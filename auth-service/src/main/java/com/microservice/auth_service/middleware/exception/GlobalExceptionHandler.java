package com.microservice.auth_service.middleware.exception;

import com.microservice.auth_service.service.util.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final LocalizationService localizationService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex, Locale locale) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), localizationService.getMessage(error.getDefaultMessage(), locale))
        );
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", localizationService.getMessage(ex.getMessage(), locale)));
    }

    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<Map<String, String>> handleJwtValidationException(JwtValidationException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", localizationService.getMessage("error.jwt.invalid", locale)));
    }

    @ExceptionHandler(AuthorizationExceptionHandler.UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserExists(AuthorizationExceptionHandler.UserAlreadyExistsException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", localizationService.getMessage(ex.getMessage(), locale)));
    }

    @ExceptionHandler(AuthorizationExceptionHandler.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(AuthorizationExceptionHandler.InvalidCredentialsException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", localizationService.getMessage(ex.getMessage(), locale)));
    }

    @ExceptionHandler(AuthorizationExceptionHandler.RoleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoleNotFound(AuthorizationExceptionHandler.RoleNotFoundException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", localizationService.getMessage(ex.getMessage(), locale)));
    }
}
