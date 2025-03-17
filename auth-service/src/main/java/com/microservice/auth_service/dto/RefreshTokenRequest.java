package com.microservice.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh-токен ID не должен быть пустым")
    private String refreshTokenUUID;
}
