package com.microservice.auth_service.controller;

import com.microservice.auth_service.model.Role;
import com.microservice.auth_service.model.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(userRepository.findAll());
    }

    @PostMapping("/user/{userId}/role")
    public ResponseEntity<String> addUserRole(@PathVariable UUID userId, @RequestBody Map<String, String> request) {
        String roleName = request.get("roleName");

        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findByName(roleName);

        if (userOpt.isEmpty() || roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь или роль не найдены");
        }

        User user = userOpt.get();
        Role newRole = roleOpt.get();

        // Добавляем роль, если её еще нет
        if (!user.getRoles().contains(newRole)) {
            user.getRoles().add(newRole);
            userRepository.save(user);
            return ResponseEntity.ok("Роль " + roleName + " добавлена пользователю " + user.getEmail());
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Роль " + roleName + " уже назначена пользователю.");
        }
    }

    @DeleteMapping("/user/{userId}/role")
    public ResponseEntity<String> removeUserRole(@PathVariable UUID userId, @RequestBody Map<String, String> request) {
        String roleName = request.get("roleName");

        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findByName(roleName);

        if (userOpt.isEmpty() || roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Пользователь или роль не найдены");
        }

        User user = userOpt.get();
        Role removeRole = roleOpt.get();

        if (!user.getRoles().contains(removeRole)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("У пользователя нет роли " + roleName);
        }

        user.getRoles().remove(removeRole);
        userRepository.save(user); //

        return ResponseEntity.ok("Роль " + roleName + " успешно удалена у пользователя " + user.getEmail());
    }



}
