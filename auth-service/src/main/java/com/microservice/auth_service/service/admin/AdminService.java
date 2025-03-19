package com.microservice.auth_service.service.admin;

import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.service.util.LocalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LocalizationService localizationService;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public ResponseEntity<String> addUserRole(UUID userId, String roleName, Locale locale) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findByName(roleName);

        if (userOpt.isEmpty() || roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(localizationService.getMessage("error.user_or_role.not_found", locale));
        }

        User user = userOpt.get();
        Role newRole = roleOpt.get();

        if (!user.getRoles().contains(newRole)) {
            user.getRoles().add(newRole);
            userRepository.save(user);
            return ResponseEntity.ok(localizationService.getMessage("success.role.added", locale, roleName, user.getEmail()));
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(localizationService.getMessage("error.role.already_assigned", locale, roleName));
        }
    }

    public ResponseEntity<String> removeUserRole(UUID userId, String roleName, Locale locale) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findByName(roleName);

        if (userOpt.isEmpty() || roleOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(localizationService.getMessage("error.user_or_role.not_found", locale));
        }

        User user = userOpt.get();
        Role removeRole = roleOpt.get();

        if (!user.getRoles().contains(removeRole)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(localizationService.getMessage("error.role.not_assigned", locale, roleName));
        }

        user.getRoles().remove(removeRole);
        userRepository.save(user);

        return ResponseEntity.ok(localizationService.getMessage("success.role.removed", locale, roleName, user.getEmail()));
    }
}
