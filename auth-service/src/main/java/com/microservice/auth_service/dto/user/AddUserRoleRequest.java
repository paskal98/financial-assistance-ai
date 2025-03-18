package com.microservice.auth_service.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserRoleRequest {
    @NotBlank(message = "{validation.role.empty}")
    private String roleName;
}
