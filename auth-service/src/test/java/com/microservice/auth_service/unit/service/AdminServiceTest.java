package com.microservice.auth_service.unit.service;

import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.admin.AdminService;
import com.microservice.auth_service.service.util.LocalizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private LocalizationService localizationService;

    @InjectMocks
    private AdminService adminService;

    private User user;
    private Role role;
    private Locale locale;

    @BeforeEach
    void setUp() {
        user = new User(UUID.randomUUID(), "test@example.com", "password", false, new HashSet<>(), false, null, null);
        role = new Role(1, "ADMIN");
        locale = Locale.ENGLISH;
    }

    @Test
    void getAllUsers_success() {
        List<User> users = List.of(user);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = adminService.getAllUsers();

        assertEquals(users, result);
    }

    @Test
    void addUserRole_success() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(localizationService.getMessage("success.role.added", locale, "ADMIN", user.getEmail())).thenReturn("Role added");

        ResponseEntity<String> response = adminService.addUserRole(user.getId(), "ADMIN", locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Role added", response.getBody());
        verify(userRepository).save(user);
    }

    @Test
    void addUserRole_alreadyAssigned() {
        user.getRoles().add(role);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(localizationService.getMessage("error.role.already_assigned", locale, "ADMIN")).thenReturn("Role already assigned");

        ResponseEntity<String> response = adminService.addUserRole(user.getId(), "ADMIN", locale);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Role already assigned", response.getBody());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeUserRole_success() {
        user.getRoles().add(role);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(localizationService.getMessage("success.role.removed", locale, "ADMIN", user.getEmail())).thenReturn("Role removed");

        ResponseEntity<String> response = adminService.removeUserRole(user.getId(), "ADMIN", locale);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Role removed", response.getBody());
        verify(userRepository).save(user);
    }
}