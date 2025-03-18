package com.microservice.auth_service.service.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LocalizationService {
    private final MessageSource messageSource;

    public String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (Exception e) {
            return code;
        }
    }
}
