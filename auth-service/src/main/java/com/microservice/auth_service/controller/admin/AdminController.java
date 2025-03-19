package com.microservice.auth_service.controller.admin;

import com.microservice.auth_service.model.dto.user.AddUserRoleRequest;
import com.microservice.auth_service.model.dto.user.RemoveUserRoleRequest;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(adminService.getAllUsers());
    }

    @PostMapping("/user/{userId}/role")
    public ResponseEntity<String> addUserRole(@PathVariable UUID userId,
                                              @Valid @RequestBody AddUserRoleRequest request,
                                              Locale locale) {
        return adminService.addUserRole(userId, request.getRoleName(), locale);
    }

    @DeleteMapping("/user/{userId}/role")
    public ResponseEntity<String> removeUserRole(@PathVariable UUID userId,
                                                 @Valid @RequestBody RemoveUserRoleRequest request,
                                                 Locale locale) {
        return adminService.removeUserRole(userId, request.getRoleName(), locale);
    }
}
