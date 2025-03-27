package com.microservice.auth_service.integration.service;

import com.microservice.auth_service.integration.BaseIntegrationTest;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.service.admin.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    @Transactional
    void addUserRole_integration() {
        User user = createUser("test@example.com", "password", Set.of("USER"));

        ResponseEntity<String> response = adminService.addUserRole(user.getId(), "ADMIN", Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Role ADMIN has been added"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertTrue(updatedUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN")));
    }

    @Test
    @Transactional
    void removeUserRole_integration() {
        User user = createUser("test@example.com", "password", Set.of("ADMIN"));

        ResponseEntity<String> response = adminService.removeUserRole(user.getId(), "ADMIN", Locale.ENGLISH);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Role ADMIN has been removed"));

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertFalse(updatedUser.getRoles().stream().anyMatch(role -> role.getName().equals("ADMIN")));
    }
}
