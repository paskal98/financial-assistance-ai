package com.microservice.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    @Email(message = "Некорректный email")
    @NotBlank(message = "Email не должен быть пустым")
    private String email;

    @NotBlank(message = "Пароль не должен быть пустым")
    @Size(min = 6, max = 30, message = "Пароль должен содержать от 6 до 30 символов")
    private String password;

    private Integer otpCode;

    private String backupCode;
}
