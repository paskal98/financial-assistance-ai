package com.miscroservice.transaction_service.integration.auth;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.miscroservice.transaction_service.security.JwtUtil;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class JwtAuthFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void testValidToken() throws Exception {
        // Генерируем валидный UUID
        String userId = UUID.randomUUID().toString();

        // Мокаем поведение JwtUtil
        Mockito.when(jwtUtil.validateToken("mock-token")).thenReturn(true);
        Mockito.when(jwtUtil.extractUserId("mock-token")).thenReturn(userId);

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk());
    }

    @Test
    void testInvalidToken() throws Exception {
        Mockito.when(jwtUtil.validateToken("mock-token")).thenReturn(false);

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isUnauthorized());
    }
}