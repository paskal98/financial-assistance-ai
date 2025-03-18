# 📌 Documentation for Auth-Service

## 📍 General Overview
`auth-service` is a microservice responsible for user authentication and authorization in the AI-Powered Personal Finance Assistant. It supports:
- Registration and login via email + password
- OAuth2 (Google, GitHub, Apple - optional)
- User and role management
- JWT tokens (access and refresh)
- Two-factor authentication (2FA)
- Error and message localization

## 🏗️ Architecture
`auth-service` is built with **Spring Boot**, using **PostgreSQL** as the database and **Spring Security** for security. The main components:
- **Controllers** (`controller`): Handle HTTP requests.
- **Services** (`service`): Business logic for users, roles, and tokens.
- **Repositories** (`repository`): Work with the database using Spring Data JPA.
- **Configuration** (`config`): Security, localization, and OAuth2 settings.

## 🔗 API Endpoints

### 🔹 Authentication and Registration (`/auth`)
#### **1. Register a new user**
`POST /auth/register`
```json
{
  "email": "user@example.com",
  "password": "securePassword"
}
```
**Response:**
```json
{
  "token": "jwt-access-token",
  "refreshToken": "jwt-refresh-token",
  "message": "Registration successful."
}
```

#### **2. User login**
`POST /auth/login`
```json
{
  "email": "user@example.com",
  "password": "securePassword"
}
```
**Response:** Same as `register`

#### **3. Refresh access token**
`POST /auth/refresh`
```json
{
  "refreshTokenUUID": "token-id"
}
```

### 🔹 OAuth2 (`/oauth2`)
#### **1. OAuth2 login**
`GET /oauth2/callback/{provider}`

**Example:** `GET /oauth2/callback/google`

**Response:**
```json
{
  "token": "jwt-access-token",
  "provider": "google"
}
```

### 🔹 User Management (`/user`)
#### **1. Get current user information**
`GET /user/me`
**Response:**
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "roles": ["USER"]
}
```

### 🔹 Role Management (`/admin`)
Requires **ADMIN** role

#### **1. Get a list of users**
`GET /admin/users`

#### **2. Assign a role to a user**
`POST /admin/user/{userId}/role`
```json
{
  "roleName": "ADMIN"
}
```

#### **3. Remove a role from a user**
`DELETE /admin/user/{userId}/role`
```json
{
  "roleName": "ADMIN"
}
```

## 🗄️ Data Models
### **User** (Table `users`)
```java
public class User {
    private UUID id; // Unique user ID
    private String email; // User email
    private String password; // Hashed password
    private boolean oauth; // Whether OAuth is used
    private Set<Role> roles; // User roles
    private boolean is2FAEnabled; // Two-factor authentication enabled
    private String twoFASecret; // 2FA secret key
}
```

### **Role** (Table `roles`)
```java
public class Role {
    private Integer id; // Unique role ID
    private String name; // Role name (USER, ADMIN)
}
```

### **RefreshToken** (Table `refresh_tokens`)
```java
public class RefreshToken {
    private UUID id; // Unique token ID
    private User user; // Associated user
    private UUID token; // Refresh token
    private Instant expiryDate; // Token expiration date
}
```

## 📦 Docker & Build Instructions
### 🔹 **Build & Run with Docker**
#### **1. Build the JAR file**
```sh
./gradlew clean build
```

#### **2. Create Docker image**
```sh
docker build -t auth-service .
```

#### **3. Run the container**
```sh
docker run -p 8081:8081 --name auth-service auth-service
```

#### **4. Run with Docker Compose**
Create `docker-compose.yml`:
```yaml
version: '3.8'
services:
  auth-service:
    image: auth-service
    ports:
      - "8081:8081"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/auth_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
    depends_on:
      - db
  db:
    image: postgres
    restart: always
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
```
Run the application:
```sh
docker-compose up -d
```

## 🔒 Security
- JWT tokens (access and refresh)
- **BCrypt** password encryption
- **Spring Security** for access control
- OAuth2 for third-party authentication
- 2FA with QR codes and OTP (Google Authenticator)


## 🔒 Key Classes and Responsibilities
### 📌 **Controllers** (`controller`)
- **AuthController** – Handles login, registration, token refresh.
- **OAuth2Controller** – Manages OAuth2 login (Google, GitHub, etc.).
- **UserController** – User management (profile, 2FA).
- **AdminController** – User role management (admin only).

### 📌 **Services** (`service`)
- **AuthService** – Authentication logic, registration, tokens.
- **OAuth2Service** – OAuth2 login handling.
- **UserService** – User management.
- **AdminService** – Role management.
- **RefreshTokenService** – Handles refresh tokens.
- **TwoFactorAuthService** – Two-factor authentication.
- **BackupCodeService** – Generates and validates 2FA backup codes.

### 📌 **Filters and Security**
- **JwtAuthFilter** – Handles JWT tokens.
- **JwtUtil** – JWT generation and validation.
- **SecurityConfig** – Spring Security configuration.

### 📌 **Repositories** (`repository`)
- **UserRepository** – Works with `users` table.
- **RoleRepository** – Works with `roles` table.
- **RefreshTokenRepository** – Works with `refresh_tokens` table.

### 📌 **Utilities** (`util`)
- **LocalizationService** – Manages message localization.
- **QRCodeService** – Generates QR codes for 2FA.

## 🔒 Security
- JWT tokens (access and refresh)
- **BCrypt** password encryption
- **Spring Security** for access control
- OAuth2 for third-party authentication
- 2FA with QR codes and OTP (Google Authenticator)

## ⚙️ Configuration (`application.properties`)
```properties
server.port=8081
spring.datasource.url=jdbc:postgresql://localhost:5432/auth_db
jwt.secret=secret_key
jwt.expiration=86400000
jwt.expiration.refresh=604800000
spring.security.oauth2.client.registration.google.client-id=xxx
spring.security.oauth2.client.registration.google.client-secret=yyy
```

## 📢 Error Localization
Files:
- `messages.properties` (English)
- `messages_ru.properties` (Russian)

## 🎯 Conclusion
This `auth-service` provides secure registration, authentication, user management, and OAuth2 support. It is a key service for authentication in the AI-Powered Personal Finance Assistant.

