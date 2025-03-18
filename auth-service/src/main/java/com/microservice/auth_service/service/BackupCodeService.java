package com.microservice.auth_service.service;

import com.microservice.auth_service.model.User;
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

    // Генерация новых резервных кодов (5 штук)
    public List<String> generateBackupCodes(User user) {
        List<String> codes = IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID().toString().substring(0, 8))
                .collect(Collectors.toList());

        // Хешируем коды перед сохранением
        List<String> hashedCodes = codes.stream()
                .map(passwordEncoder::encode)
                .collect(Collectors.toList());

        user.setBackupCodes(hashedCodes);
        userRepository.save(user);

        return codes; // Отдаем пользователю НЕ захешированные коды
    }

    // Проверка введенного кода
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
