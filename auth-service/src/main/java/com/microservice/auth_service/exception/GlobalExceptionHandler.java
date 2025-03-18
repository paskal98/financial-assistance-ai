package com.microservice.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Обрабатываем ошибки валидации DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }

    // Общий обработчик RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(JwtValidationException.class)
    public ResponseEntity<Map<String, String>> handleJwtValidationException(JwtValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationExceptionHandler.UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserExists(AuthorizationExceptionHandler.UserAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationExceptionHandler.InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(AuthorizationExceptionHandler.InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AuthorizationExceptionHandler.RoleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRoleNotFound(AuthorizationExceptionHandler.RoleNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", ex.getMessage()));
    }

}
