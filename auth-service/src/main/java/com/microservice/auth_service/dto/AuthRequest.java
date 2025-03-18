package com.microservice.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthRequest {
    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.email.empty}")
    private String email;

    @NotBlank(message = "{validation.password.empty}")
    @Size(min = 6, max = 30, message = "{validation.password.length}")
    private String password;

    private Integer otpCode;
    private String backupCode;
}
