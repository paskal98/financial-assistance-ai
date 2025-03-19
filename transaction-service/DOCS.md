# 📌 Конфигурационный модуль `transaction-service`

## 📍 Общее описание
Конфигурационный модуль `transaction-service` отвечает за безопасность, взаимодействие с Redis и интеграцию с другими сервисами через OpenFeign. Включает:
- **RedisConfig** — настройка Redis для кэширования данных.
- **SecurityConfig** — конфигурация Spring Security (JWT-аутентификация).
- **OpenFeignConfig** — настройка OpenFeign для взаимодействия с `auth-service`.

---

## 🔹 `RedisConfig`
**Пакет:** `config`

### 📌 Функциональность:
- Определяет `RedisTemplate` для работы с Redis.
- Использует `Jackson2JsonRedisSerializer` для сериализации объектов.
- Настраивает сериализацию ключей через `StringRedisSerializer`.

### 📌 Используемые компоненты:
- `RedisConnectionFactory` — подключение к Redis.
- `RedisTemplate<String, Object>` — основное API взаимодействия с Redis.
- `Jackson2JsonRedisSerializer<Object>` — сериализация данных в JSON.

**Фрагмент кода:**
```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());

    ObjectMapper objectMapper = new ObjectMapper();
    Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    template.setValueSerializer(serializer);

    return template;
}
```

---

## 🔹 `SecurityConfig`
**Пакет:** `config`

### 📌 Функциональность:
- Отключает CSRF (для REST API).
- Настраивает JWT-аутентификацию через `JwtAuthFilter`.
- Разрешает публичный доступ к `/actuator/**` (для мониторинга).

### 📌 Используемые компоненты:
- `JwtAuthFilter` — фильтр проверки JWT-токенов.
- `JwtUtil` — утилита для работы с JWT.
- `SecurityFilterChain` — конфигурация Spring Security.

**Фрагмент кода:**
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

---

## 🔹 `OpenFeignConfig`
**Пакет:** `config`

### 📌 Функциональность:
- Настраивает OpenFeign для взаимодействия с `auth-service`.
- Добавляет поддержку декодирования ошибок Feign.

### 📌 Используемые компоненты:
- `@EnableFeignClients` — включает OpenFeign.
- `ErrorDecoder` — обработка ошибок Feign-клиентов.

**Фрагмент кода:**
```java
@Configuration
@EnableFeignClients(basePackages = "com.miscroservice.transaction_service.client")
public class OpenFeignConfig {
}
```

---

## 🎯 Итог
Этот модуль отвечает за преднастройку `transaction-service`, включая безопасность (JWT), кэширование (Redis) и интеграцию с `auth-service` (OpenFeign).



# 📌 Контроллеры, сервисы и репозитории в `transaction-service`

## 📍 Общее описание
Этот модуль отвечает за управление категориями транзакций, их хранение в БД и кэширование в Redis. Включает:
- **CategoryController** — обработка HTTP-запросов к категориям.
- **CategoryService** и **CategoryServiceImpl** — бизнес-логика управления категориями.
- **CategoryRepository** — доступ к БД.
- **CategoryResponse** — DTO для передачи данных о категориях.
- **Category** — сущность категории в базе данных.

---

## 🔹 `CategoryController`
**Пакет:** `controller`

### 📌 Функциональность:
- Обрабатывает HTTP-запросы, связанные с категориями.
- Поддерживает фильтрацию по типу категории (`income`, `expense`).

### 📌 Основные эндпоинты:
- `GET /categories` — Получить список категорий (с фильтрацией по типу).

### 📌 Используемые компоненты:
- `CategoryService` — сервис для получения категорий.

**Фрагмент кода:**
```java
@GetMapping
public ResponseEntity<List<CategoryResponse>> getCategories(@RequestParam(required = false) String type) {
    List<CategoryResponse> categories = categoryService.getCategories(type);
    return ResponseEntity.ok(categories);
}
```

---

## 🔹 `CategoryService` и `CategoryServiceImpl`
**Пакет:** `service`

### 📌 Функциональность:
- Получает список категорий из базы данных.
- Использует Redis для кэширования результатов.
- Позволяет фильтровать категории по типу.

### 📌 Используемые компоненты:
- `CategoryRepository` — доступ к БД.
- `RedisTemplate<String, Object>` — кэширование данных в Redis.

### 📌 Логика работы кэширования:
1. Проверяется наличие категорий в Redis по ключу `categories:{type}`.
2. Если данные есть — возвращаются из кэша.
3. Если данных нет — загружаются из БД и сохраняются в кэше на 1 час.

**Фрагмент кода:**
```java
@Override
public List<CategoryResponse> getCategories(String type) {
    String cacheKey = CATEGORIES_CACHE_PREFIX + (type != null ? type : "all");
    @SuppressWarnings("unchecked")
    List<CategoryResponse> cachedCategories = (List<CategoryResponse>) redisTemplate.opsForValue().get(cacheKey);

    if (cachedCategories != null) {
        return cachedCategories;
    }

    List<CategoryResponse> categories;
    if (type != null) {
        categories = categoryRepository.findAllByType(type).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                .toList();
    } else {
        categories = categoryRepository.findAll().stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                .toList();
    }

    redisTemplate.opsForValue().set(cacheKey, categories, 1, TimeUnit.HOURS);
    return categories;
}
```

---

## 🔹 `CategoryRepository`
**Пакет:** `repository`

### 📌 Функциональность:
- Обеспечивает доступ к таблице `categories`.
- Поддерживает поиск категорий по имени и типу.

### 📌 Используемые методы:
- `Optional<Category> findByName(String name)` — поиск категории по имени.
- `List<Category> findAllByType(String type)` — фильтрация категорий по типу.

**Фрагмент кода:**
```java
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    Optional<Category> findByName(String name);
    List<Category> findAllByType(String type);
}
```

---

## 🔹 `CategoryResponse` (DTO)
**Пакет:** `model.dto`

### 📌 Функциональность:
- Используется для передачи информации о категории.
- Включает идентификатор, название и тип категории.

**Фрагмент кода:**
```java
@Data
@NoArgsConstructor
public class CategoryResponse {
    private Integer id;
    private String name;
    private String type;

    public CategoryResponse(Integer id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
```

---

## 🔹 `Category` (Сущность БД)
**Пакет:** `model.entity`

### 📌 Функциональность:
- Определяет структуру таблицы `categories`.
- Хранит название и тип категории.

**Фрагмент кода:**
```java
@Entity
@Table(name = "categories")
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String type;
}
```

---

## 🎯 Итог
Этот модуль управляет категориями транзакций, обрабатывая их через API, храня в БД и кэшируя в Redis. Контроллер взаимодействует с `CategoryService`, который запрашивает данные через `CategoryRepository



# 📌 Контроллеры, сервисы и репозитории в `transaction-service`

## 📍 Общее описание
Этот модуль управляет транзакциями пользователей, обеспечивая создание, обновление, удаление и статистику по расходам и доходам. Включает:
- **TransactionController** — API-контроллер для обработки запросов к транзакциям.
- **TransactionService** и **TransactionServiceImpl** — бизнес-логика обработки транзакций.
- **TransactionRepository** — доступ к БД.
- **TransactionRequest** и **TransactionResponse** — DTO для передачи данных.
- **Transaction** — сущность транзакции в базе данных.

---

## 🔹 `TransactionController`
**Пакет:** `controller`

### 📌 Функциональность:
- Обрабатывает запросы на создание, получение, обновление и удаление транзакций.
- Позволяет фильтровать транзакции по дате, категории и типу.
- Предоставляет статистику по доходам и расходам.

### 📌 Основные эндпоинты:
- `POST /transactions` — Создать транзакцию.
- `GET /transactions` — Получить список транзакций с фильтрацией.
- `PUT /transactions/{id}` — Обновить транзакцию.
- `DELETE /transactions/{id}` — Удалить транзакцию.
- `GET /transactions/stats` — Получить статистику транзакций.

**Фрагмент кода:**
```java
@PostMapping
public ResponseEntity<TransactionResponse> createTransaction(
        @RequestBody TransactionRequest request,
        @AuthenticationPrincipal String userId) {
    TransactionResponse response = transactionService.createTransaction(request, UUID.fromString(userId));
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

## 🔹 `TransactionService` и `TransactionServiceImpl`
**Пакет:** `service`

### 📌 Функциональность:
- Управляет транзакциями (создание, обновление, удаление, получение).
- Использует Redis для кэширования списков транзакций и статистики.
- Поддерживает фильтрацию транзакций по дате, категории и типу.

### 📌 Используемые компоненты:
- `TransactionRepository` — доступ к БД.
- `RedisTemplate<String, Object>` — кэширование в Redis.
- `CategoryRepository` — проверка наличия категории перед сохранением транзакции.

**Фрагмент кода:**
```java
@Override
public TransactionResponse createTransaction(TransactionRequest request, UUID userId) {
    validateCategory(request.getCategory());
    validateType(request.getType());
    Transaction transaction = new Transaction();
    transaction.setUserId(userId);
    transaction.setAmount(request.getAmount());
    transaction.setType(request.getType());
    transaction.setCategory(request.getCategory());
    transaction.setDescription(request.getDescription());
    transaction.setDate(Instant.parse(request.getDate()));
    transaction.setCreatedAt(Instant.now());
    transaction.setUpdatedAt(Instant.now());
    transaction = transactionRepository.save(transaction);
    invalidateCache(userId);
    return mapToResponse(transaction);
}
```

---

## 🔹 `TransactionRepository`
**Пакет:** `repository`

### 📌 Функциональность:
- Управляет хранением транзакций в БД.
- Поддерживает фильтрацию по дате, категории, типу.

### 📌 Используемые методы:
- `List<Transaction> findByUserId(UUID userId)` — поиск всех транзакций пользователя.
- `Page<Transaction> findByFilters(...)` — фильтрация транзакций по параметрам.

**Фрагмент кода:**
```java
@Query("SELECT t FROM Transaction t WHERE t.userId = :userId " +
        "AND (:startDate IS NULL OR t.date >= :startDate) " +
        "AND (:endDate IS NULL OR t.date <= :endDate) " +
        "AND (:category IS NULL OR t.category = :category) " +
        "AND (:type IS NULL OR t.type = :type)")
Page<Transaction> findByFilters(UUID userId, Instant startDate, Instant endDate, String category, String type, Pageable pageable);
```

---

## 🔹 `TransactionRequest` и `TransactionResponse` (DTO)
**Пакет:** `model.dto`

### 📌 `TransactionRequest`
Используется для передачи данных при создании/обновлении транзакции.
```java
@Data
public class TransactionRequest {
    @NotNull
    private BigDecimal amount;
    @NotBlank
    private String type;
    @NotBlank
    private String category;
    private String description;
    @NotBlank
    private String date;
    private String paymentMethod;
}
```

### 📌 `TransactionResponse`
Используется для отправки данных о транзакции клиенту.
```java
@Data
@NoArgsConstructor
public class TransactionResponse {
    private UUID id;
    private BigDecimal amount;
    private String type;
    private String category;
    private String description;
    private String date;
}
```

---

## 🔹 `Transaction` (Сущность БД)
**Пакет:** `model.entity`

### 📌 Функциональность:
- Определяет структуру таблицы `transactions`.
- Хранит данные о пользователе, сумме, типе, категории и дате.

**Фрагмент кода:**
```java
@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String category;

    @Column
    private String description;

    @Column(nullable = false)
    private Instant date;
}
```

---

## 🎯 Итог
Этот модуль управляет транзакциями пользователей, обеспечивая их создание, обновление, удаление и анализ. API `TransactionController` взаимодействует с `TransactionService`, который обрабатывает данные через `TransactionRepository`. Использование Redis позволяет сократить нагрузку на БД и повысить производительность сервиса.





# 📌 Обработка исключений в `transaction-service`

## 📍 Общее описание
Этот модуль управляет обработкой исключений в `transaction-service`. Он включает:
- **AccessDeniedException** — выбрасывается при отсутствии прав доступа.
- **TransactionNotFoundException** — выбрасывается, если транзакция не найдена.
- **GlobalExceptionHandler** — глобальный обработчик исключений.

---

## 🔹 `AccessDeniedException`
**Пакет:** `exception`

### 📌 Функциональность:
- Выбрасывается, если пользователь пытается изменить или удалить чужую транзакцию.

**Фрагмент кода:**
```java
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
```

---

## 🔹 `TransactionNotFoundException`
**Пакет:** `exception`

### 📌 Функциональность:
- Выбрасывается, если запрашиваемая транзакция не найдена в базе данных.

**Фрагмент кода:**
```java
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException() {
        super("Transaction not found");
    }
}
```

---

## 🔹 `GlobalExceptionHandler`
**Пакет:** `exception`

### 📌 Функциональность:
- Обрабатывает исключения глобально для всех контроллеров.
- Возвращает корректные HTTP-статусы и сообщения об ошибках.

### 📌 Основные обработчики:
- `handleTransactionNotFound` → Возвращает **404 NOT FOUND**, если транзакция не найдена.
- `handleAccessDenied` → Возвращает **403 FORBIDDEN**, если у пользователя нет доступа.
- `handleIllegalArgument` → Возвращает **400 BAD REQUEST**, если переданы некорректные данные.
- `handleGenericException` → Возвращает **500 INTERNAL SERVER ERROR**, если произошла непредвиденная ошибка.

**Фрагмент кода:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<String> handleTransactionNotFound(TransactionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred");
    }
}
```

---

## 🎯 Итог
Этот модуль обеспечивает централизованную обработку ошибок в `transaction-service`, улучшая безопасность и удобочитаемость API. Он возвращает четкие HTTP-ответы для различных сценариев ошибок, помогая клиентам API корректно реагировать на исключения.



# 📌 Безопасность и аутентификация в `transaction-service`

## 📍 Общее описание
Этот модуль управляет аутентификацией пользователей с помощью JWT-токенов. Включает:
- **JwtAuthFilter** — фильтр проверки JWT-токенов.
- **JwtUtil** — утилита для генерации и валидации JWT.

---

## 🔹 `JwtAuthFilter`
**Пакет:** `security`

### 📌 Функциональность:
- Проверяет наличие заголовка `Authorization` в запросе.
- Извлекает и валидирует JWT-токен.
- Устанавливает пользователя в `SecurityContextHolder` при успешной проверке токена.

### 📌 Логика работы:
1. Проверяет, есть ли в заголовке `Authorization` JWT-токен.
2. Извлекает токен и валидирует его через `JwtUtil`.
3. Если токен корректен, извлекает `userId` и устанавливает пользователя в `SecurityContextHolder`.

**Фрагмент кода:**
```java
@Override
protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            String userId = jwtUtil.extractUserId(token);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userId, null, null);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
    filterChain.doFilter(request, response);
}
```

---

## 🔹 `JwtUtil`
**Пакет:** `security`

### 📌 Функциональность:
- Извлекает `userId` из JWT-токена.
- Валидирует токен с помощью секретного ключа.

### 📌 Используемые компоненты:
- `@Value("${jwt.secret}")` — получение секретного ключа из `application.properties`.
- `Jwts.parser()` — парсинг и валидация JWT.

**Фрагмент кода:**
```java
public String extractUserId(String token) {
    return Jwts.parser()
            .setSigningKey(secret)
            .parseClaimsJws(token)
            .getBody()
            .get("userId", String.class);
}
```

```java
public boolean validateToken(String token) {
    try {
        Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
        return true;
    } catch (Exception e) {
        return false;
    }
}
```

---

## 🎯 Итог
Этот модуль обеспечивает безопасность `transaction-service`, аутентифицируя пользователей с помощью JWT. `JwtAuthFilter` проверяет токен в каждом запросе, а `JwtUtil` валидирует токен и извлекает `userId`.


# 📌 Конфигурация `transaction-service`

## 📍 Общее описание
Этот модуль содержит основные настройки `transaction-service`, включая параметры сервера, базы данных, Redis, Kafka, Eureka и JWT-аутентификации.

---

## 🔹 Основные параметры
**Файл:** `application.properties`

### 📌 `Server Configuration`
Определяет параметры работы сервера Spring Boot.
```properties
server.port=8082
spring.application.name=transaction-service
```

### 📌 `Database Configuration`
Настройки подключения к PostgreSQL.
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/transactions_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
```

### 📌 `JPA Configuration`
Настройки Hibernate и отображение SQL-запросов.
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 📌 `JWT Configuration`
Настройки безопасности для аутентификации пользователей через JWT.
```properties
jwt.secret=AAAAcwAAAGUAAABjAAAAcgAAAGUAAAB0AAAAXwAAAGsAAABlAAAAeQ==
jwt.expiration=86400000
jwt.expiration.refresh=604800000
```

### 📌 `Redis Configuration`
Настройки кэширования через Redis.
```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### 📌 `Kafka Configuration`
Настройки брокера сообщений Apache Kafka.
```properties
spring.kafka.bootstrap-servers=localhost:9092
```

### 📌 `Eureka Configuration`
Настройки для работы с сервисной регистрацией Eureka.
```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
```

---

## 🎯 Итог
Файл `application.properties` содержит все основные конфигурационные параметры для работы `transaction-service`, включая базу данных, кэширование, микросервисную интеграцию и безопасность.


# 📌 Конфигурация `transaction-service`

## 📍 Общее описание
Этот модуль содержит основные настройки `transaction-service`, включая параметры сервера, базы данных, Redis, Kafka, Eureka и JWT-аутентификации.

---

## 🔹 Основные параметры
**Файл:** `application.properties`

### 📌 `Server Configuration`
Определяет параметры работы сервера Spring Boot.
```properties
server.port=8082
spring.application.name=transaction-service
```

### 📌 `Database Configuration`
Настройки подключения к PostgreSQL.
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/transactions_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver
```

### 📌 `JPA Configuration`
Настройки Hibernate и отображение SQL-запросов.
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 📌 `JWT Configuration`
Настройки безопасности для аутентификации пользователей через JWT.
```properties
jwt.secret=AAAAcwAAAGUAAABjAAAAcgAAAGUAAAB0AAAAXwAAAGsAAABlAAAAeQ==
jwt.expiration=86400000
jwt.expiration.refresh=604800000
```

### 📌 `Redis Configuration`
Настройки кэширования через Redis.
```properties
spring.redis.host=localhost
spring.redis.port=6379
```

### 📌 `Kafka Configuration`
Настройки брокера сообщений Apache Kafka.
```properties
spring.kafka.bootstrap-servers=localhost:9092
```

### 📌 `Eureka Configuration`
Настройки для работы с сервисной регистрацией Eureka.
```properties
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
```

---

## 📂 Дерево проекта
```plaintext
transaction-service/
│── src/
│   ├── main/
│   │   ├── java/com/miscroservice/transaction_service/
│   │   │   ├── controller/
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── TransactionController.java
│   │   │   ├── service/
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── CategoryServiceImpl.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   ├── TransactionServiceImpl.java
│   │   │   ├── repository/
│   │   │   │   ├── CategoryRepository.java
│   │   │   │   ├── TransactionRepository.java
│   │   │   ├── model/
│   │   │   │   ├── dto/
│   │   │   │   │   ├── CategoryResponse.java
│   │   │   │   │   ├── TransactionRequest.java
│   │   │   │   │   ├── TransactionResponse.java
│   │   │   │   │   ├── TransactionStatsResponse.java
│   │   │   │   ├── entity/
│   │   │   │   │   ├── Category.java
│   │   │   │   │   ├── Transaction.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   ├── JwtUtil.java
│   │   │   ├── exception/
│   │   │   │   ├── AccessDeniedException.java
│   │   │   │   ├── TransactionNotFoundException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── config/
│   │   │   │   ├── OpenFeignConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   ├── resources/
│   │   │   ├── application.properties
│── build.gradle
│── README.md (если есть)
```

### 🔹 Основные компоненты:
- **controller/** – REST-контроллеры (`CategoryController`, `TransactionController`).
- **service/** – Бизнес-логика (`TransactionService`, `CategoryService`).
- **repository/** – Доступ к базе данных (`TransactionRepository`, `CategoryRepository`).
- **model/**
    - `dto/` – DTO-объекты для API (`TransactionRequest`, `TransactionResponse`, `TransactionStatsResponse`).
    - `entity/` – JPA-сущности (`Transaction`, `Category`).
- **security/** – Защита с помощью JWT (`JwtAuthFilter`, `JwtUtil`).
- **exception/** – Обработка исключений (`AccessDeniedException`, `TransactionNotFoundException`, `GlobalExceptionHandler`).
- **config/** – Конфигурация сервиса (`SecurityConfig`, `RedisConfig`, `OpenFeignConfig`).
- **resources/** – Конфигурационные файлы (`application.properties`).

---

## 🎯 Итог
Файл `application.properties` содержит все основные конфигурационные параметры для работы `transaction-service`, включая базу данных, кэширование, микросервисную интеграцию и безопасность. Структура проекта построена по принципам Spring Boot и микросервисной архитектуры.


