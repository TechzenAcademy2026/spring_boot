# student_management_api_app

Spring Boot API App for student management

---

## Tech Stack

- **Java 21** + Spring Boot 3.x
- **Spring Data JPA** + PostgreSQL
- **Flyway** (database migration)
- **Spring Security** + JWT
- **Lombok**, MapStruct, springdoc-openapi (Swagger UI)

---

## Getting Started

### Prerequisites

- Java 21
- Docker Desktop

### Chạy database development

```bash
docker-compose up -d
```

### Chạy ứng dụng

```bash
./gradlew bootRun
```

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## Testing

### Hai loại test

| | Unit Test | Integration Test |
|---|---|---|
| **File** | `PersonServiceTest` | `PersonServiceIntegrationTest` |
| **Công cụ** | Mockito (mock) | Testcontainers (DB thật) |
| **Database** | Không cần | PostgreSQL container |
| **Tốc độ** | Rất nhanh | Chậm hơn (cần pull Docker image) |
| **Mục đích** | Test logic đơn lẻ | Test luồng thật từ service → DB |

---

### Config Integration Test với Testcontainers

Để chạy integration test với PostgreSQL thật, cần cấu hình ở **4 nơi**:

---

#### 1. `build.gradle` — Thêm dependency

Thêm **Testcontainers** vào `dependencies` (chỉ dùng cho test, không ảnh hưởng production):

```groovy
// Testcontainers BOM quản lý version tập trung cho tất cả module
testImplementation platform('org.testcontainers:testcontainers-bom:1.20.4')

// Tích hợp với JUnit 5 lifecycle (start/stop container tự động)
testImplementation 'org.testcontainers:junit-jupiter'

// Module PostgreSQL cho Testcontainers
testImplementation 'org.testcontainers:postgresql'
```

---

#### 2. `src/test/resources/test-schema.sql` — Khởi tạo schema trong container

Testcontainers spin-up một PostgreSQL container **trắng hoàn toàn**. Entity `Person` được map vào schema `app` (`@Table(schema = "app")`), nên cần tạo schema trước khi Hibernate tạo bảng:

```sql
CREATE SCHEMA IF NOT EXISTS app;
```

File này được chỉ định trong class test qua `.withInitScript("test-schema.sql")`.

---

#### 3. `src/test/resources/application-test.properties` — Config Spring cho môi trường test

File này chỉ được load khi test class có `@ActiveProfiles("test")`. Cần cấu hình:

```properties
# Tắt Flyway: migration scripts không cần chạy trong test
# (Flyway giữ cho production DB, Hibernate ddl-auto giữ cho test DB)
spring.flyway.enabled=false

# Hibernate tự tạo bảng trước test và xóa sau khi Spring context đóng
spring.jpa.hibernate.ddl-auto=create-drop

# Hibernate cần biết default schema là "app" để tạo bảng đúng chỗ
spring.jpa.properties.hibernate.default_schema=app

# Không cần config datasource ở đây —
# @DynamicPropertySource trong class test sẽ inject URL/user/password
# của PostgreSQL container lúc runtime

# Tắt Spring Security: không cần xác thực khi test service
spring.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,\
  org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
```

---

#### 4. `PersonServiceIntegrationTest.java` — Khai báo container và kết nối Spring

Đây là nơi **kết nối Testcontainers với Spring**:

```java
@SpringBootTest        // Load full Spring context
@ActiveProfiles("test") // Load application-test.properties
@Transactional         // Mỗi test tự ROLLBACK → test độc lập
@Testcontainers        // Bật Testcontainers JUnit 5 extension
class PersonServiceIntegrationTest {

    // Khai báo container — static để share cho cả class (chỉ start 1 lần)
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withInitScript("test-schema.sql"); // chạy file SQL ở bước 2

    // Inject URL/user/password của container vào Spring datasource
    // Phương thức này chạy TRƯỚC khi Spring context được khởi tạo
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
```

---

### Tạo Dummy Data trong Integration Test

Thay vì dùng file SQL fixture, dummy data được **tạo bằng code Java** ngay trong test. Cách này linh hoạt hơn và dễ đọc hơn.

**Cơ chế:**  
`@Transactional` trên class test tạo ra **một transaction chung** cho `@BeforeEach` và mỗi `@Test`. Khi test kết thúc, toàn bộ transaction được **ROLLBACK tự động** — không cần xóa data thủ công, mỗi test luôn bắt đầu với DB sạch.

```java
@Autowired
private PersonRepository personRepository; // inject thẳng repository để tạo data

private Person savedPerson; // giữ tham chiếu để dùng trong @Test

@BeforeEach
void setUp() {
    // Tạo dummy data và lưu vào PostgreSQL container
    Person dummy = Person.builder()
            .fullName("Nguyen Van A")
            .dob(LocalDate.of(2000, 5, 15))
            .phone("0901234567")
            .contactEmail("nguyenvana@example.com")
            .address("123 Nguyen Trai, Ha Noi")
            .build();

    savedPerson = personRepository.saveAndFlush(dummy);
    // saveAndFlush(): lưu ngay xuống DB (flush), không chờ transaction commit
    // → các câu lệnh SELECT tiếp theo trong cùng test thấy được data này
}
```

Trong mỗi `@Test`, `savedPerson` đã sẵn sàng để dùng:

```java
@Test
void shouldReturnPersonDetail_whenIdExists() {
    // Act — service gọi repo → query PostgreSQL container
    PersonDetailResponse result = personService.getById(savedPerson.getId());

    // Assert
    assertThat(result.fullName()).isEqualTo("Nguyen Van A");
}
```

---

### Chạy test

> ⚠️ **Docker Desktop phải đang chạy** trước khi chạy integration test.

```bash
# Chỉ chạy integration test
./gradlew test --tests "*.PersonServiceIntegrationTest"

# Chạy tất cả tests
./gradlew test
```

Báo cáo HTML: `build/reports/tests/test/index.html`
