package com.microservice.auth_service.integration;

import com.microservice.auth_service.model.entity.Role;
import com.microservice.auth_service.model.entity.User;
import com.microservice.auth_service.repository.RefreshTokenRepository;
import com.microservice.auth_service.repository.RoleRepository;
import com.microservice.auth_service.repository.UserRepository;
import com.microservice.auth_service.security.JwtUtil;
import com.microservice.auth_service.service.admin.AdminService;
import com.microservice.auth_service.service.authentication.AuthService;
import com.microservice.auth_service.service.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("auth_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
            .withExposedPorts(6379);

    @Autowired protected AuthService authService;
    @Autowired protected JwtUtil jwtUtil;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected RedisTemplate<String, String> redisTemplate;
    @Autowired protected AdminService adminService;
    @Autowired protected UserService userService;
    @Autowired protected RoleRepository roleRepository;
    @Autowired protected EntityManager entityManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @BeforeEach
    protected void setUp() {
        clearDatabaseAndCache();
        initDefaultRoles();
    }

    @AfterEach
    protected void tearDown() {
        clearDatabaseAndCache();
    }

    private void clearDatabaseAndCache() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();
    }

    private void initDefaultRoles() {
        roleRepository.saveAll(List.of(
                new Role(null, "USER"),
                new Role(null, "ADMIN")
        ));
    }

    protected User createUser(String email, String password, Set<String> roleNames) {
        Set<Role> roles = roleNames.stream()
                .map(roleName -> roleRepository.findByName(roleName).orElseThrow())
                .collect(Collectors.toSet());

        User user = new User(null, email, password, false, roles, false, null, null);
        return userRepository.save(user);
    }
}
