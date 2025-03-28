package com.microservice.auth_service.service.authentication;

import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class BackupCodeService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<String> generateBackupCodes(User user) {
        List<String> codes = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString().substring(0, 8))
                .collect(Collectors.toList());

        List<String> hashedCodes = codes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toList());

        user.setBackupCodes(hashedCodes);
        userRepository.save(user);

        return codes;
    }

    public boolean validateBackupCode(User user, String inputCode) {
        List<String> hashedCodes = user.getBackupCodes();

        for (String hashedCode : hashedCodes) {
            if (passwordEncoder.matches(inputCode, hashedCode)) {
                hashedCodes.remove(hashedCode);
                userRepository.save(user);
                return true;
            }
        }

        return false;
    }
}
