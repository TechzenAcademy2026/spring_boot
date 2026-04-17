package student.management.api_app.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.dto.person.PersonListItemResponse;
import student.management.api_app.dto.person.PersonSearchRequest;
import student.management.api_app.service.IPersonService;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho PersonService.search() sử dụng PostgreSQL thật
 * (Testcontainers).
 *
 * Luồng hoạt động:
 * 1. @Testcontainers + @Container → JUnit 5 lifecycle: tự động start/stop
 * container.
 * 2. withInitScript("test-schema.sql") → tạo schema "app" ngay sau khi
 * container ready.
 * 3. @DynamicPropertySource → inject JDBC URL vào cả Spring datasource VÀ
 * Flyway.
 * Quan trọng: phải override spring.flyway.url (application.properties có URL
 * riêng trỏ localhost).
 * 4. application-test.properties → Flyway ĐƯỢC PHÉP chạy → migrate schema +
 * seed 30 người.
 * 5. Tests chạy search() trên 30 bản ghi seed — không cần insert thủ công.
 * 6. @Transactional(readOnly = true) → test chỉ đọc, không thay đổi DB.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional(readOnly = true)
@Testcontainers
class PersonServiceIntegrationTest {

    // =========================================================
    // Testcontainers — PostgreSQL Container
    // =========================================================

    /**
     * static → container được share cho toàn bộ class, chỉ khởi động 1 lần.
     * withInitScript("test-schema.sql") → tạo schema "app" TRƯỚC khi Flyway chạy.
     */
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withInitScript("test-schema.sql");

    /**
     * Inject thông tin kết nối của container vào Spring datasource VÀ Flyway.
     *
     * Lý do phải override spring.flyway.url:
     * application.properties có
     * spring.flyway.url=jdbc:postgresql://localhost:5432/...
     * Nếu không override, Flyway sẽ migrate vào DB production (localhost) thay vì
     * container!
     */
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        // Spring datasource
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Flyway phải dùng cùng container (override spring.flyway.url từ
        // application.properties)
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    /** Service thật: chạy business logic → repository → PostgreSQL container. */
    @Autowired
    private IPersonService personService;

    // =========================================================
    // search() Tests
    // =========================================================

    @Nested
    @DisplayName("search() Tests")
    class SearchTests {

        @Test
        @DisplayName("GIVEN name='Nguyen' WHEN search THEN trả về đúng 3 người có 'Nguyen' trong tên")
        void shouldReturnThreePeople_whenSearchByNameNguyen() {
            // Arrange — 3 người có "nguyen" (case-insensitive): Nguyen Van An, Nguyen Thi
            // Chau, Nguyen Linh
            PersonSearchRequest req = new PersonSearchRequest(
                    "Nguyen", null, null, null, null, null);

            // Act
            PageResponse<PersonListItemResponse> result = personService.search(req, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getItems()).hasSize(3);
            assertThat(result.getItems())
                    .extracting(PersonListItemResponse::fullName)
                    .containsExactlyInAnyOrder("Nguyen Van An", "Nguyen Thi Chau", "Nguyen Linh");
        }

        @Test
        @DisplayName("GIVEN phone='0905000003' WHEN search THEN trả về đúng 1 người (Le Hoang Cuong)")
        void shouldReturnOnePerson_whenSearchByExactPhone() {
            // Arrange
            PersonSearchRequest req = new PersonSearchRequest(
                    null, "0905000003", null, null, null, null);

            // Act
            PageResponse<PersonListItemResponse> result = personService.search(req, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).fullName()).isEqualTo("Le Hoang Cuong");
        }

        @Test
        @DisplayName("GIVEN dobFrom=2007-01-01 & dobTo=2008-12-31 WHEN search THEN trả về 2 người sinh 2007–2008")
        void shouldReturnTwoPeople_whenSearchByDobRange2007To2008() {
            // Arrange — dob trong khoảng 2007–2008: Nguyen Van An (2007-05-12), Do Minh Gia
            // (2008-01-14)
            PersonSearchRequest req = new PersonSearchRequest(
                    null, null, null, null,
                    LocalDate.of(2007, 1, 1), // dobFrom
                    LocalDate.of(2008, 12, 31) // dobTo
            );

            // Act
            PageResponse<PersonListItemResponse> result = personService.search(req, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getItems()).hasSize(2);
            assertThat(result.getItems())
                    .extracting(PersonListItemResponse::fullName)
                    .containsExactlyInAnyOrder("Nguyen Van An", "Do Minh Gia");
        }

        @Test
        @DisplayName("GIVEN dobFrom=2006-01-01 WHEN search THEN trả về đúng 5 người sinh từ 2006 trở đi")
        void shouldReturnFivePeople_whenSearchByDobFrom2006() {
            // Arrange — 5 người sinh từ 2006-01-01:
            // Nguyen Van An (2007-05-12), Pham Thi Diep (2006-11-03), Do Minh Gia
            // (2008-01-14)
            // Le Hai (2006-05-25), Le Vy (2006-02-23)
            PersonSearchRequest req = new PersonSearchRequest(
                    null, null, null, null,
                    LocalDate.of(2006, 1, 1), // dobFrom
                    null // dobTo = không giới hạn
            );

            // Act
            PageResponse<PersonListItemResponse> result = personService.search(req, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getItems()).hasSize(5);
            assertThat(result.getItems())
                    .extracting(PersonListItemResponse::fullName)
                    .containsExactlyInAnyOrder(
                            "Nguyen Van An", "Pham Thi Diep", "Do Minh Gia",
                            "Le Hai", "Le Vy");
        }

        @Test
        @DisplayName("GIVEN tất cả params null WHEN search THEN trả về toàn bộ 30 người")
        void shouldReturnAllThirtyPeople_whenAllParamsAreNull() {
            // Arrange — không có filter nào → trả về toàn bộ seed data (10 từ V1.0.1 + 20
            // từ V1.0.5)
            PersonSearchRequest req = new PersonSearchRequest(
                    null, null, null, null, null, null);

            // Act
            PageResponse<PersonListItemResponse> result = personService.search(req, PageRequest.of(0, 50));

            // Assert
            assertThat(result.getTotalItems()).isEqualTo(30);
        }

        @Test
        @DisplayName("GIVEN name không tồn tại WHEN search THEN trả về danh sách rỗng")
        void shouldReturnEmpty_whenNameNotFound() {
            // Arrange
            PersonSearchRequest req = new PersonSearchRequest(
                    "XYZ_KHONG_TON_TAI", null, null, null, null, null);

            // Act
            PageResponse<PersonListItemResponse> result = personService.search(req, PageRequest.of(0, 10));

            // Assert
            assertThat(result.getItems()).isEmpty();
            assertThat(result.getTotalItems()).isEqualTo(0);
        }
    }
}
