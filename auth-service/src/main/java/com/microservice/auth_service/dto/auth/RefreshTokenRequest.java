package com.microservice.auth_service.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "{validation.refresh_token.empty}")
    private String refreshTokenUUID;
}
