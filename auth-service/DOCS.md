# 📌 Конфигурационный модуль `auth-service`

## 📍 Общее описание
Конфигурационный модуль отвечает за инициализацию базы данных, локализацию и безопасность сервиса. Включает:
- **DatabaseSeeder** — предзаполнение БД ролями (`USER`, `ADMIN`).
- **MessageConfig** — настройка i18n (локализация ошибок и сообщений).
- **SecurityConfig** — конфигурация Spring Security (JWT, OAuth2, роли).

---

## 🔹 `DatabaseSeeder`
**Пакет:** `config.database`

### 📌 Функциональность:
- При старте приложения проверяет наличие ролей в БД.
- Если `USER` и `ADMIN` отсутствуют, добавляет их в `roles`.

### 📌 Используемые компоненты:
- `RoleRepository` для работы с таблицей `roles`.
- `@PostConstruct` для выполнения метода `init()` при старте приложения.

---

## 🔹 `MessageConfig`
**Пакет:** `config.messages`

### 📌 Функциональность:
- Определяет `LocaleResolver` для поддержки локализации.
- Настраивает `MessageSource` для загрузки сообщений из `classpath:i18n/messages`.
- Используется для интернационализации сообщений об ошибках и уведомлений.

### 📌 Используемые компоненты:
- `ReloadableResourceBundleMessageSource` для загрузки локализованных строк.
- `AcceptHeaderLocaleResolver` для определения локали на основе заголовка запроса.

---

## 🔹 `SecurityConfig`
**Пакет:** `config.security`

### 📌 Функциональность:
- Отключает CSRF (для REST API).
- Настраивает JWT-аутентификацию (`JwtAuthFilter`).
- Определяет роли и права доступа (`hasAuthority("ADMIN")`).
- Конфигурирует OAuth2 (редирект после успешного входа).

### 📌 Используемые компоненты:
- `JwtAuthFilter` (фильтр проверки JWT-токенов).
- `OAuth2SuccessHandler` (обработка OAuth2 входа).
- `BCryptPasswordEncoder` (хеширование паролей).
- `SecurityFilterChain` для настройки доступа.

---

## 🎯 Итог
Этот модуль отвечает за преднастройку приложения перед его запуском: наполнение БД ролями, настройку локализации и конфигурацию безопасности.


# 📌 Конфигурация контроллеров в Auth-Service

## 🔹 AdminController
**Описание:** Управляет административными операциями, такими как управление пользователями и ролями.

**Основные эндпоинты:**
- `GET /admin/users` — Получить список всех пользователей.
- `POST /admin/user/{userId}/role` — Добавить роль пользователю.
- `DELETE /admin/user/{userId}/role` — Удалить роль у пользователя.

**Примечания:**
- Требует роль **ADMIN**.
- Использует `AdminService` для обработки запросов.

---

## 🔹 AuthController
**Описание:** Отвечает за аутентификацию пользователей (регистрация, вход, выход, обновление токена).

**Основные эндпоинты:**
- `POST /auth/register` — Регистрация нового пользователя.
- `POST /auth/login` — Аутентификация пользователя.
- `POST /auth/refresh` — Обновление JWT-токена.
- `POST /auth/logout` — Выход из системы.

**Примечания:**
- Использует `AuthService` для работы с пользователями и токенами.
- Поддерживает локализацию ошибок через `LocalizationService`.

---

## 🔹 OAuth2Controller
**Описание:** Обрабатывает аутентификацию через сторонние OAuth2-провайдеры (Google, GitHub и др.).

**Основные эндпоинты:**
- `GET /oauth2/callback/{provider}` — Обработчик успешного входа через OAuth2.

**Примечания:**
- Использует `OAuth2Service` для генерации JWT после успешного входа.
- Перенаправляет пользователя на фронтенд после успешной аутентификации.

---

## 🔹 UserController
**Описание:** Управляет операциями, связанными с пользователем (профиль, 2FA, резервные коды).

**Основные эндпоинты:**
- `GET /user/me` — Получить информацию о текущем пользователе.
- `POST /user/2fa/enable` — Включить двухфакторную аутентификацию.
- `POST /user/2fa/disable` — Отключить 2FA.
- `POST /user/2fa/backup-codes` — Сгенерировать резервные коды для входа.
- `POST /user/2fa/disable-with-backup` — Отключить 2FA с использованием резервного кода.

**Примечания:**
- Использует `UserService`, `TwoFactorAuthService` и `BackupCodeService`.
- Проверяет аутентификацию пользователя через `@AuthenticationPrincipal`.

---

## 🔹 Итог
Этот модуль контроллеров обеспечивает управление пользователями, ролями, аутентификацией и OAuth2. Каждый контроллер делегирует обработку сервисным слоям, соблюдая принцип **Separation of Concerns**.


# 📌 DTO Конфигурация в Auth-Service

## 🔹 Общая информация
DTO (Data Transfer Object) — это объекты для передачи данных между слоями. В `auth-service` используются DTO для передачи информации при аутентификации, управлении токенами и ролями пользователей.

## 🔹 DTO Классы

### **1. AuthRequest**
Используется для регистрации и аутентификации пользователей.
```java
public class AuthRequest {
    private String email;   // Email пользователя
    private String password; // Пароль (min 6, max 30 символов)
    private Integer otpCode;  // Одноразовый код для 2FA (если включено)
    private String backupCode; // Резервный код для отключения 2FA
}
```
🔹 **Валидация:**
- `@Email` — проверка на корректный email.
- `@NotBlank` — поле не может быть пустым.
- `@Size(min = 6, max = 30)` — ограничение на длину пароля.

---

### **2. RefreshTokenRequest**
Используется для запроса нового access-токена.
```java
public class RefreshTokenRequest {
    private String refreshTokenUUID; // UUID рефреш-токена
}
```
🔹 **Валидация:**
- `@NotBlank` — токен обязателен.

---

### **3. AddUserRoleRequest**
Используется для назначения новой роли пользователю.
```java
public class AddUserRoleRequest {
    private String roleName; // Название роли (например, ADMIN, USER)
}
```
🔹 **Валидация:**
- `@NotBlank` — название роли обязательно.

---

### **4. RemoveUserRoleRequest**
Используется для удаления роли у пользователя.
```java
public class RemoveUserRoleRequest {
    private String roleName; // Название роли, которую нужно удалить
}
```
🔹 **Валидация:**
- `@NotBlank` — название роли обязательно.

## 🔹 Итог
Эти DTO обеспечивают корректную передачу данных между клиентом и сервером, минимизируя риски ошибок при валидации входящих запросов.


# 📌 Middleware Configuration Documentation

## 🔹 Overview
This document describes the core middleware components handling security, authentication, and exception management in the `auth-service`. It provides a concise technical reference for developers and AI models like GPT.

## 🔹 Middleware Components

### 1️⃣ **AuthorizationExceptionHandler** (Exception Handling)
Handles authentication and authorization-related exceptions. Defines custom exceptions to ensure structured error management.

- `UserAlreadyExistsException` – Thrown when attempting to register an existing user.
- `RoleNotFoundException` – Raised when trying to assign/remove a non-existent role.
- `InvalidCredentialsException` – Triggered when authentication fails due to incorrect credentials.

These exceptions are consumed by the `GlobalExceptionHandler` for standardized error responses.

### 2️⃣ **GlobalExceptionHandler** (Centralized Exception Management)
Centralizes exception handling across the application, ensuring consistent error responses.

- Catches validation errors (`MethodArgumentNotValidException`) and returns localized messages.
- Handles `JwtValidationException`, ensuring unauthorized requests receive proper status codes.
- Manages exceptions from `AuthorizationExceptionHandler`, returning structured error messages with appropriate HTTP status codes.

### 3️⃣ **JwtAuthFilter** (JWT Authentication)
Filters incoming requests to validate JWT tokens before they reach protected endpoints.

- Extracts the token from the `Authorization` header.
- Uses `JwtUtil` to parse and validate the token.
- If valid, retrieves user details and sets `SecurityContextHolder`.
- Skips authentication for public endpoints (`/auth/**`, `/oauth2/**`).

## 🔹 Summary
These middleware components form the security backbone of `auth-service`, managing authentication, exception handling, and JWT validation. Redis integration enhances performance and security.

Next steps: Expand documentation with Redis-based optimizations and additional middleware configurations.



# 📌 Конфигурационные модели и репозитории в `auth-service`

## 🏗️ Модели данных

### 🔹 `RefreshToken`
**Описание**: Управляет токенами обновления (refresh tokens), используется для повторной аутентификации без запроса пароля.
```java
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue
    private UUID id; // Уникальный идентификатор токена

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Связанный пользователь

    @Column(nullable = false, unique = true)
    private UUID token; // Сам токен

    @Column(nullable = false)
    private Instant expiryDate; // Дата истечения токена
}
```

### 🔹 `Role`
**Описание**: Определяет роли пользователей (USER, ADMIN и т. д.).
```java
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // Уникальный идентификатор роли

    @Column(unique = true, nullable = false)
    private String name; // Название роли
}
```

### 🔹 `User`
**Описание**: Основная модель пользователя.
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue
    private UUID id; // Уникальный идентификатор пользователя

    @Column(unique = true, nullable = false)
    private String email; // Email пользователя

    @Column(nullable = false)
    private String password; // Захешированный пароль

    @Column(nullable = false)
    private boolean oauth; // Флаг использования OAuth

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles; // Список ролей пользователя

    @Column(name = "is_2fa_enabled", nullable = false)
    private boolean is2FAEnabled; // Включена ли 2FA

    @Column(name = "two_fa_secret")
    private String twoFASecret; // Секретный ключ для 2FA
}
```

---

## 🗄️ Репозитории

### 🔹 `RefreshTokenRepository`
**Описание**: Управляет хранением и очисткой рефреш-токенов.
```java
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findById(UUID id); // Поиск токена по ID
    void deleteByUser(User user); // Удаление токенов конкретного пользователя

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    void deleteAllByExpiryDateBefore(Instant now); // Удаление устаревших токенов

    List<RefreshToken> findAllByExpiryDateBefore(Instant now); // Найти все устаревшие токены
}
```

### 🔹 `RoleRepository`
**Описание**: Операции с ролями пользователей.
```java
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name); // Найти роль по названию
}
```

### 🔹 `UserRepository`
**Описание**: Управляет пользователями.
```java
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email); // Найти пользователя по email
}
```

---

## 🎯 Итог
Этот модуль определяет основные **сущности** (пользователи, роли, токены) и **репозитории**, обеспечивающие их хранение и управление. `RefreshTokenRepository` также включает **очистку устаревших токенов** через аннотированный `@Query` запрос.



# 🔐 Security Configuration in Auth-Service

## 🔹 Overview
The `auth-service` security configuration ensures authentication and authorization using JWT and OAuth2. The core components include:
- **JwtUtil** – JWT token generation and validation.
- **OAuth2SuccessHandler** – Handling OAuth2 authentication flow.

## 🛠 JwtUtil
**Location:** `com.microservice.auth_service.security.JwtUtil`

### 🔹 Responsibilities:
- Generates access tokens for authenticated users.
- Extracts and validates claims from JWT.
- Handles token expiration and invalid token scenarios.

### 🔹 Key Methods:
```java
public String generateToken(String email);
```
- Creates a JWT token for the given email.
- Signed with **HS256** using a Base64-encoded secret key.
- Sets expiration based on `jwt.expiration` in `application.properties`.

```java
public boolean validateToken(String token);
```
- Parses and validates the JWT token.
- Throws specific exceptions for expired, malformed, or invalid tokens.

```java
public String extractEmail(String token);
```
- Retrieves the subject (email) from a valid token.

---
## 🛠 OAuth2SuccessHandler
**Location:** `com.microservice.auth_service.security.OAuth2SuccessHandler`

### 🔹 Responsibilities:
- Processes successful OAuth2 authentication.
- Registers new users (if email not found in the database).
- Generates a JWT token and redirects to the frontend with authentication details.

### 🔹 Key Logic:
```java
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication);
```
- Extracts user details from the `OAuth2AuthenticationToken`.
- If the user is new, creates a database entry with the `USER` role.
- Generates a JWT and redirects the user to `frontendUrlBase/oauth-success?token=jwt`.

## ⚙️ Configuration References
**JWT Settings (`application.properties`):**
```properties
jwt.secret=base64_encoded_secret
jwt.expiration=86400000
jwt.expiration.refresh=604800000
```

**OAuth2 Providers:**
```properties
spring.security.oauth2.client.registration.google.client-id=xxx
spring.security.oauth2.client.registration.google.client-secret=yyy
spring.security.oauth2.client.registration.google.redirect-uri={baseUrl}/oauth2/callback/google
```

## 🔎 Summary
- `JwtUtil` manages JWT authentication.
- `OAuth2SuccessHandler` handles OAuth2 login flow.
- Secure token-based authentication with role-based access control (RBAC).

This configuration ensures seamless authentication and secure access control across the system.



# 📌 Документация по конфигурационным сервисам: AdminService, CustomUserDetailsService, UserService

## **📍 Общее описание**
Этот документ описывает **AdminService**, **CustomUserDetailsService** и **UserService** в `auth-service`. Эти сервисы обеспечивают управление пользователями, их ролями и взаимодействие с Spring Security.

---

## **🔹 AdminService**
**Ответственность:**
- Управление пользователями и их ролями (назначение и удаление ролей).
- Получение списка всех пользователей.
- Взаимодействие с `UserRepository` и `RoleRepository`.

### **Методы:**
#### 📌 `getAllUsers() -> List<User>`
Возвращает список всех пользователей из базы данных.

#### 📌 `addUserRole(UUID userId, String roleName, Locale locale) -> ResponseEntity<String>`
- Добавляет роль пользователю, если она не назначена.
- Проверяет существование пользователя и роли.
- Локализует сообщения об ошибках и успехе.

#### 📌 `removeUserRole(UUID userId, String roleName, Locale locale) -> ResponseEntity<String>`
- Удаляет роль у пользователя.
- Проверяет, есть ли у пользователя указанная роль.

---

## **🔹 CustomUserDetailsService**
**Ответственность:**
- Интеграция `User` с Spring Security.
- Загрузка пользователя по email для аутентификации.
- Преобразование `User` в `UserDetails` для Spring Security.

### **Методы:**
#### 📌 `loadUserByUsername(String email) -> UserDetails`
- Загружает пользователя по email из `UserRepository`.
- Преобразует его в `UserDetails`, включая роли (GrantedAuthority).
- Выбрасывает `UsernameNotFoundException`, если email отсутствует в БД.

---

## **🔹 UserService**
**Ответственность:**
- Управление профилем пользователя.
- Управление двухфакторной аутентификацией (2FA).
- Генерация резервных кодов для 2FA.

### **Методы:**
#### 📌 `findByEmail(String email) -> Optional<User>`
Возвращает пользователя по email, если он существует.

#### 📌 `enable2FA(User user, Locale locale) -> Map<String, String>`
- Генерирует секретный ключ для 2FA.
- Создает QR-код для Google Authenticator.
- Сохраняет настройки 2FA для пользователя.

#### 📌 `disable2FA(User user, Locale locale) -> Map<String, String>`
Отключает 2FA и удаляет секретный ключ.

#### 📌 `generateBackupCodes(User user, Locale locale) -> Map<String, Object>`
- Создает и сохраняет резервные коды для 2FA.
- Возвращает их в ответе.

#### 📌 `disable2FAWithBackupCode(User user, String backupCode, Locale locale) -> Map<String, String>`
- Проверяет резервный код 2FA.
- Если код верный, отключает 2FA.

---

## **📌 Итог**
- **AdminService** — управление пользователями и ролями.
- **CustomUserDetailsService** — аутентификация пользователей в Spring Security.
- **UserService** — управление профилем и 2FA.

Эти сервисы обеспечивают ключевые функции безопасности и управления пользователями в `auth-service`. Документ содержит только необходимую информацию без лишних деталей.



# 📌 Конфигурационные сервисы и утилиты

## 📍 LocalizationService
### 📌 Описание
`LocalizationService` предоставляет поддержку мультиязычности в `auth-service`, управляя локализованными сообщениями через Spring `MessageSource`. Используется для генерации сообщений об ошибках, успешных операциях и системных уведомлениях.

### 🔹 Основной функционал:
- Получение локализованных сообщений по коду.
- Поддержка аргументов в локализованных строках.
- Обработка отсутствующих ключей локализации (fallback на сам код).

### 🛠️ Ключевые методы:
```java
public String getMessage(String code, Locale locale, Object... args);
```
- **`code`** — ключ сообщения в `messages.properties`.
- **`locale`** — язык пользователя (например, `Locale.ENGLISH`).
- **`args`** — аргументы для вставки в локализованное сообщение.

Пример использования:
```java
String message = localizationService.getMessage("error.user.not_found", Locale.ENGLISH);
```

---

## 📍 QRCodeService
### 📌 Описание
`QRCodeService` отвечает за генерацию QR-кодов, используемых для двухфакторной аутентификации (2FA). Основан на **ZXing** (Google's QR Code Generator).

### 🔹 Основной функционал:
- Генерация QR-кода в **PNG**.
- Возвращение QR-кода в **Base64** для удобной передачи на фронтенд.

### 🛠️ Ключевые методы:
```java
public byte[] generateQRCode(String otpAuthUrl);
```
- **`otpAuthUrl`** — URL-строка в формате **otpauth://**, используемая в Google Authenticator.
- **Возвращает** массив байтов изображения QR-кода в формате PNG.

```java
public String generateQRCodeBase64(String otpAuthUrl);
```
- **Возвращает** QR-код в Base64-строке для прямого отображения в вебе (`<img src="data:image/png;base64,..."/>`).

Пример использования:
```java
String qrBase64 = qrCodeService.generateQRCodeBase64("otpauth://totp/..."));
```

---

## 🎯 Итог
Эти сервисы обеспечивают:
- **LocalizationService** — удобную работу с мультиязычностью.
- **QRCodeService** — генерацию QR-кодов для 2FA.
  Они играют вспомогательную роль в `auth-service`, но критичны для UX и безопасности.



# 📌 Документация: Конфигурационные сервисы аутентификации

## 🔹 Обзор
Этот документ описывает конфигурационные сервисы аутентификации в `auth-service`, включая работу с refresh-токенами, двухфакторной аутентификацией и OAuth2.

---
## 🔐 **RefreshTokenService** (Работа с Refresh-токенами)
### 📌 **Основные функции**
- Управляет жизненным циклом refresh-токенов.
- Использует **PostgreSQL** и **Redis** для хранения и кэширования токенов.
- Обеспечивает безопасное обновление access-токенов.

### 🛠 **Функционал**
- `createRefreshToken(User user)`: Создает новый refresh-токен, удаляет старый, сохраняет в БД и кэширует в Redis.
- `findById(String token)`: Ищет токен сначала в Redis, затем в PostgreSQL.
- `validateRefreshToken(String rawTokenUUID, RefreshToken storedToken)`: Проверяет валидность refresh-токена.
- `deleteByUser(User user)`: Удаляет все refresh-токены пользователя из PostgreSQL и Redis.
- `deleteExpiredTokens()`: По расписанию удаляет истекшие токены из базы и кэша.

### 🛠 **Технологии**
- **PostgreSQL** → Основное хранилище токенов.
- **Redis** → Быстрый доступ к актуальным токенам.
- **Spring Scheduler** → Автоматическая очистка просроченных токенов.

---
## 🔑 **TwoFactorAuthService** (Двухфакторная аутентификация)
### 📌 **Основные функции**
- Реализует **Google Authenticator** для 2FA.
- Генерирует **QR-коды** для настройки OTP-аутентификации.
- Верифицирует одноразовые пароли (TOTP) и резервные коды.

### 🛠 **Функционал**
- `generateSecretKey(User user, Locale locale)`: Генерирует секретный ключ 2FA и сохраняет в БД.
- `getQRCode(User user, Locale locale)`: Возвращает QR-код для настройки Google Authenticator.
- `verifyCode(User user, int code)`: Проверяет введенный OTP-код.
- `disable2FA(User user)`: Отключает 2FA и удаляет секретный ключ пользователя.

### 🛠 **Технологии**
- **Google Authenticator API** → Генерация и верификация TOTP-кодов.
- **ZXing** → Генерация QR-кодов.

---
## 🔄 **OAuth2Service** (OAuth2 Аутентификация)
### 📌 **Основные функции**
- Интегрирует **Google, GitHub, Apple OAuth2**.
- Обрабатывает аутентификацию через внешних провайдеров.
- Генерирует JWT-токен после успешного входа через OAuth.

### 🛠 **Функционал**
- `processOAuthCallback(OAuth2AuthenticationToken token, String provider, Locale locale)`: Обрабатывает OAuth-токен и возвращает JWT-токен.

### 🛠 **Технологии**
- **Spring Security OAuth2** → Работа с OAuth-провайдерами.
- **JWT** → Генерация access-токенов после успешной OAuth-аутентификации.

---
## 📌 Итог
Эти сервисы обеспечивают **безопасную аутентификацию** пользователей, включая **refresh-токены, OAuth2 и двухфакторную защиту**. Они оптимизированы для быстрого доступа (через Redis) и надежного хранения (в PostgreSQL), что делает систему масштабируемой и безопасной.




# 📌 Документация: Конфигурационный сервис `AuthService`

## 📍 Общее описание
`AuthService` — сервис, отвечающий за аутентификацию пользователей в системе. Реализует:
- **Регистрацию** новых пользователей.
- **Аутентификацию** по email/паролю.
- **Верификацию 2FA** (если включено).
- **Работу с JWT** (доступ + рефреш токены).
- **Обновление токена** по рефрешу.
- **Выход** из системы.

## ⚙️ Основные зависимости
- `UserRepository` — работа с пользователями.
- `RoleRepository` — роли пользователей.
- `JwtUtil` — генерация и валидация JWT.
- `RefreshTokenService` — управление рефреш-токенами.
- `TwoFactorAuthService` — 2FA аутентификация.
- `BackupCodeService` — валидация бэкап-кодов 2FA.
- `PasswordEncoder` — хеширование паролей.

## 🔹 Ключевые методы
### **1. Регистрация пользователя**
`public Map<String, String> register(String email, String password, Locale locale)`
- Проверяет, существует ли пользователь.
- Назначает роль `USER`.
- Хеширует пароль.
- Создает JWT и рефреш-токен.

### **2. Вход в систему**
`public Map<String, String> login(AuthRequest request, Locale locale)`
- Проверяет пользователя и пароль.
- Валидирует 2FA (OTP/бэкап-коды).
- Создает JWT + рефреш-токен.

### **3. Обновление токена**
`public Map<String, String> refreshToken(String refreshTokenUUID, Locale locale)`
- Проверяет, валиден ли рефреш-токен.
- Генерирует новый access-токен.

### **4. Выход**
`public void logout(String userEmail, Locale locale)`
- Удаляет рефреш-токен пользователя.

## 🔑 Логика 2FA
Если у пользователя включен 2FA:
1. Если передан `backupCode`, проверяется по `BackupCodeService`.
2. Если передан `otpCode`, верифицируется через `TwoFactorAuthService`.
3. Если нет кода — отказ в доступе.

## 🔒 Безопасность
- **BCrypt** для хеширования паролей.
- **JWT** для аутентификации.
- **2FA** (Google Authenticator, бэкап-коды).
- **Рефреш-токены** с ограниченным временем жизни.

## 🎯 Итог
`AuthService` управляет процессами аутентификации, следит за безопасностью и токенами, поддерживает 2FA и рефреш-механику.