package com.microservice.auth_service.unit.controller;

import com.microservice.auth_service.controller.admin.AdminController;
import com.microservice.auth_service.model.dto.user.AddUserRoleRequest;
import com.microservice.auth_service.model.dto.user.RemoveUserRoleRequest;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.service.admin.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private User user;
    private Locale locale;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "test@example.com", "password", false, null, false, null, null);
        locale = Locale.ENGLISH;
    }

    @Test
    void getAllUsers_success() {
        List<User> users = List.of(user);
        when(adminService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<User>> response = adminController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(users, response.getBody());
        verify(adminService, times(1)).getAllUsers();
    }

    @Test
    void addUserRole_success() {
        AddUserRoleRequest request = new AddUserRoleRequest();
        request.setRoleName("ADMIN");
        ResponseEntity<String> serviceResponse = ResponseEntity.ok("Role added");
        when(adminService.addUserRole(user.getId(), "ADMIN", locale)).thenReturn(serviceResponse);

        ResponseEntity<String> response = adminController.addUserRole(user.getId(), request, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Role added", response.getBody());
        verify(adminService, times(1)).addUserRole(user.getId(), "ADMIN", locale);
    }

    @Test
    void removeUserRole_success() {
        RemoveUserRoleRequest request = new RemoveUserRoleRequest();
        request.setRoleName("ADMIN");
        ResponseEntity<String> serviceResponse = ResponseEntity.ok("Role removed");
        when(adminService.removeUserRole(user.getId(), "ADMIN", locale)).thenReturn(serviceResponse);

        ResponseEntity<String> response = adminController.removeUserRole(user.getId(), request, locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Role removed", response.getBody());
        verify(adminService, times(1)).removeUserRole(user.getId(), "ADMIN", locale);
    }
}