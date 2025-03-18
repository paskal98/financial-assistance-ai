package com.microservice.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RemoveUserRoleRequest {
    @NotBlank(message = "{validation.role.empty}")
    private String roleName;
}
