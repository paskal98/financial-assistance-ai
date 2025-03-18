package com.microservice.auth_service.middleware.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.microservice.auth_service.service.util.LocalizationService;

@Component
@RequiredArgsConstructor
public class AuthorizationExceptionHandler {

    private final LocalizationService localizationService;

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class RoleNotFoundException extends RuntimeException {
        public RoleNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
