package com.microservice.auth_service.integration.controller;

import com.microservice.auth_service.integration.BaseControllerIntegrationTest;
import com.microservice.auth_service.model.dto.auth.AuthRequest;
import com.microservice.auth_service.model.dto.user.AddUserRoleRequest;
import com.microservice.auth_service.model.dto.user.RemoveUserRoleRequest;
import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminControllerIntegrationTest extends BaseControllerIntegrationTest {

    private String adminToken;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        adminToken = generateAdminToken();
    }

    @Test
    void getAllUsers_integration() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders registerHeaders = new HttpHeaders();
        registerHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, registerHeaders);

        String registerUrl = "http://localhost:" + port + "/auth/register";

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String getUsersUrl = "http://localhost:" + port + "/admin/users";
        ResponseEntity<User[]> response = restTemplate.exchange(getUsersUrl, HttpMethod.GET, entity, User[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void addUserRole_integration() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders registerHeaders = new HttpHeaders();
        registerHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, registerHeaders);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        String userToken = (String) registerResponse.getBody().get("token");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

        String userMeUrl = "http://localhost:" + port + "/user/me";
        ResponseEntity<Map> userResponse = restTemplate.exchange(userMeUrl, HttpMethod.GET, userEntity, Map.class);
        assertEquals(HttpStatus.OK, userResponse.getStatusCode());

        String userId = (String) userResponse.getBody().get("id");

        AddUserRoleRequest request = new AddUserRoleRequest();
        request.setRoleName("ADMIN");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);
        HttpEntity<AddUserRoleRequest> entity = new HttpEntity<>(request, headers);

        String addRoleUrl = "http://localhost:" + port + "/admin/user/" + userId + "/role";
        ResponseEntity<String> response = restTemplate.exchange(addRoleUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("Role ADMIN has been added"));
    }

    @Test
    void addUserRole_withInvalidRole_fails() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        String userToken = (String) registerResponse.getBody().get("token");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

        String userMeUrl = "http://localhost:" + port + "/user/me";
        ResponseEntity<Map> userResponse = restTemplate.exchange(userMeUrl, HttpMethod.GET, userEntity, Map.class);
        String userId = (String) userResponse.getBody().get("id");

        AddUserRoleRequest request = new AddUserRoleRequest();
        request.setRoleName("INVALID_ROLE");

        headers.setBearerAuth(adminToken);
        HttpEntity<AddUserRoleRequest> entity = new HttpEntity<>(request, headers);

        String addRoleUrl = "http://localhost:" + port + "/admin/user/" + userId + "/role";
        ResponseEntity<String> response = restTemplate.exchange(addRoleUrl, HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("User or role not found"));
    }

    @Test
    void removeUserRole_withNonAssignedRole_fails() {
        AuthRequest registerRequest = new AuthRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);
        String userToken = (String) registerResponse.getBody().get("token");

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userToken);
        HttpEntity<Void> userEntity = new HttpEntity<>(userHeaders);

        String userMeUrl = "http://localhost:" + port + "/user/me";
        ResponseEntity<Map> userResponse = restTemplate.exchange(userMeUrl, HttpMethod.GET, userEntity, Map.class);
        String userId = (String) userResponse.getBody().get("id");

        // Attempt to remove a role that isn't assigned
        RemoveUserRoleRequest request = new RemoveUserRoleRequest();
        request.setRoleName("ADMIN");

        headers.setBearerAuth(adminToken);
        HttpEntity<RemoveUserRoleRequest> entity = new HttpEntity<>(request, headers);

        String removeRoleUrl = "http://localhost:" + port + "/admin/user/" + userId + "/role";
        ResponseEntity<String> response = restTemplate.exchange(removeRoleUrl, HttpMethod.DELETE, entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("User does not have role ADMIN"));
    }

    private String generateAdminToken() {
        AuthRequest adminRequest = new AuthRequest();
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> registerEntity = new HttpEntity<>(adminRequest, headers);

        String registerUrl = "http://localhost:" + port + "/auth/register";
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity(registerUrl, registerEntity, Map.class);

        if (registerResponse.getStatusCode() != HttpStatus.CREATED) {
            throw new RuntimeException("Failed to register admin: " + registerResponse.getStatusCode());
        }

        User admin = userRepository.findByEmail("admin@example.com").orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "ADMIN")));
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);

        HttpEntity<AuthRequest> loginEntity = new HttpEntity<>(adminRequest, headers);
        String loginUrl = "http://localhost:" + port + "/auth/login";
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(loginUrl, loginEntity, Map.class);

        if (loginResponse.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to login admin: " + loginResponse.getStatusCode());
        }

        return (String) loginResponse.getBody().get("token");
    }
}