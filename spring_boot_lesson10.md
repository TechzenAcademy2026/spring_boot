# Spring Boot – Buổi 10: Unit Testing với JUnit 5 & Mockito

## 1) Tổng quan Unit Testing

### 1.1 Unit Test là gì?

> **Unit Test** là kiểm thử đơn vị – test **một phương thức** hoặc **một hành vi** của class một cách **độc lập**, không phụ thuộc vào database, network hay các service khác.
>
> Mục tiêu: đảm bảo logic của từng method hoạt động đúng trước khi ghép vào hệ thống.

### 1.2 Tại sao cần Unit Test?

| Lợi ích                  | Mô tả                                                         |
|--------------------------|----------------------------------------------------------------|
| Phát hiện bug sớm        | Tìm lỗi ngay khi viết code, trước khi deploy                  |
| Tự tin refactor           | Sửa code mà không sợ phá vỡ logic cũ                          |
| Tài liệu sống            | Test case mô tả rõ hành vi mong đợi của method                |
| Tiết kiệm thời gian      | Test chạy trong mili-giây, không cần khởi động server          |
| CI/CD                    | Tự động chạy test khi push code, đảm bảo chất lượng liên tục  |

### 1.3 Kiến trúc Test trong Spring Boot

```
src/
├── main/java/          ← Source code chính
│   └── student/management/api_app/
│       ├── service/impl/StudentService.java
│       └── ...
├── test/java/          ← Test code
│   └── student/management/api_app/
│       ├── service/impl/StudentServiceTest.java   ← Unit test
│       └── ...
```

> **Quy ước**: Test class đặt ở **cùng package** với class cần test, nhưng nằm trong thư mục `src/test/java/`

### 1.4 Dependency đã có sẵn

Project đã có trong `build.gradle`:

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

> `spring-boot-starter-test` đã bao gồm:
> * **JUnit 5** (JUnit Jupiter) – framework test
> * **Mockito** – framework mock object
> * **AssertJ** – thư viện assertion fluent
> * **Hamcrest** – matcher library
> * **Spring Test** – hỗ trợ test Spring context

---

## 2) JUnit 5 cơ bản

### 2.1 Cấu trúc một test class

```java
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {

    @BeforeAll  // Chạy 1 lần trước TẤT CẢ test (phải static)
    static void beforeAll() {
        System.out.println("=== Bắt đầu test suite ===");
    }

    @BeforeEach // Chạy trước MỖI test method
    void setUp() {
        System.out.println("--- Setup ---");
    }

    @Test // Đánh dấu method là test case
    @DisplayName("1 + 1 phải bằng 2")
    void shouldAdd() {
        assertEquals(2, 1 + 1);
    }

    @Test
    @DisplayName("Chia cho 0 phải ném ArithmeticException")
    void shouldThrowWhenDivideByZero() {
        assertThrows(ArithmeticException.class, () -> {
            int result = 10 / 0;
        });
    }

    @AfterEach  // Chạy sau MỖI test method
    void tearDown() {
        System.out.println("--- Teardown ---");
    }

    @AfterAll   // Chạy 1 lần sau TẤT CẢ test (phải static)
    static void afterAll() {
        System.out.println("=== Kết thúc test suite ===");
    }
}
```

### 2.2 Lifecycle annotation

```
@BeforeAll ──────────────────────────────────────
    │
    ├── @BeforeEach ──→ @Test (test 1) ──→ @AfterEach
    │
    ├── @BeforeEach ──→ @Test (test 2) ──→ @AfterEach
    │
    ├── @BeforeEach ──→ @Test (test 3) ──→ @AfterEach
    │
@AfterAll ───────────────────────────────────────
```

| Annotation    | Phạm vi              | `static`? | Dùng khi                                    |
|---------------|----------------------|-----------|----------------------------------------------|
| `@BeforeAll`  | 1 lần / class        | Có        | Khởi tạo resource dùng chung (DB connect...) |
| `@BeforeEach` | Trước mỗi `@Test`   | Không     | Reset trạng thái, khởi tạo object test       |
| `@AfterEach`  | Sau mỗi `@Test`     | Không     | Dọn dẹp sau mỗi test                        |
| `@AfterAll`   | 1 lần / class        | Có        | Giải phóng resource dùng chung               |

### 2.3 Các assertion phổ biến

```java
import static org.junit.jupiter.api.Assertions.*;

// So sánh giá trị
assertEquals(expected, actual);           // actual == expected
assertEquals(expected, actual, "message"); // kèm message khi fail
assertNotEquals(unexpected, actual);       // actual != unexpected

// Kiểm tra null
assertNull(value);
assertNotNull(value);

// Kiểm tra boolean
assertTrue(condition);
assertFalse(condition);

// Kiểm tra exception
assertThrows(ExceptionClass.class, () -> { /* code ném exception */ });

// Kiểm tra không ném exception
assertDoesNotThrow(() -> { /* code bình thường */ });

// So sánh reference (cùng object)
assertSame(expected, actual);     // expected == actual (reference)
assertNotSame(expected, actual);

// Kiểm tra nhiều assertion cùng lúc (chạy hết, báo tất cả lỗi)
assertAll("Student validation",
    () -> assertNotNull(student.getId()),
    () -> assertEquals("STU001", student.getStudentCode()),
    () -> assertEquals(2024, student.getEnrollmentYear())
);
```

> **`assertAll`** rất hữu ích: thay vì dừng ở assertion fail đầu tiên, nó chạy **tất cả** và báo cáo **tất cả** lỗi cùng lúc

### 2.4 `@DisplayName` và `@Disabled`

```java
@Test
@DisplayName("Tạo student thành công khi đầy đủ thông tin")
void createStudentSuccessfully() {
    // ...
}

@Test
@Disabled("Chưa implement feature này")
void shouldSendEmailAfterCreate() {
    // Test này sẽ bị bỏ qua
}
```

### 2.5 `@Nested` – Nhóm test theo ngữ cảnh

```java
@DisplayName("StudentService Tests")
class StudentServiceTest {

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Trả về student khi tìm thấy")
        void shouldReturnStudentWhenFound() { /* ... */ }

        @Test
        @DisplayName("Ném exception khi không tìm thấy")
        void shouldThrowWhenNotFound() { /* ... */ }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Tạo thành công với dữ liệu hợp lệ")
        void shouldCreateSuccessfully() { /* ... */ }

        @Test
        @DisplayName("Ném exception khi student code đã tồn tại")
        void shouldThrowWhenCodeExists() { /* ... */ }
    }
}
```

> `@Nested` giúp tổ chức test có cấu trúc rõ ràng, dễ đọc report

### 2.6 `@ParameterizedTest` – Test nhiều input

```java
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class ValidationTests {

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a"})
    @DisplayName("Reject invalid full names")
    void shouldRejectInvalidFullName(String name) {
        // Test với từng giá trị: "", "   ", "a"
        assertThrows(IllegalArgumentException.class,
            () -> validateFullName(name));
    }

    @ParameterizedTest
    @NullAndEmptySource   // test với null và ""
    @ValueSource(strings = {"   "})
    @DisplayName("Reject blank student codes")
    void shouldRejectBlankStudentCode(String code) {
        // Test với null, "", "   "
    }

    @ParameterizedTest
    @CsvSource({
        "STU001, true",
        "STU999, true",
        "ABC001, false",
        "STU,    false",
        "'',     false"
    })
    @DisplayName("Validate student code format")
    void shouldValidateStudentCodeFormat(String code, boolean expected) {
        assertEquals(expected, isValidStudentCode(code));
    }
}
```

---

## 3) Mockito cơ bản

### 3.1 Tại sao cần Mock?

> Khi test `StudentService`, ta **không muốn** kết nối database thật.
>
> → Dùng **Mock** để tạo object giả lập cho `StudentRepository`, `PersonRepository`, `StudentMapper`...
>
> → Kiểm soát hoàn toàn dữ liệu trả về, tập trung test **logic của Service**

```
┌─────────────────────────────────────────────────────┐
│                   Unit Test                         │
│                                                     │
│  StudentService  ←──── Mock StudentRepository       │
│     (thật)       ←──── Mock PersonRepository        │
│                  ←──── Mock MajorRepository         │
│                  ←──── Mock StudentMapper            │
│                  ←──── Mock EntityManager            │
│                                                     │
│  ✅ Test logic service KHÔNG cần database thật       │
└─────────────────────────────────────────────────────┘
```

### 3.2 Setup test class với `@ExtendWith(MockitoExtension.class)`

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class) // Kích hoạt Mockito cho JUnit 5
class StudentServiceTest {

    @Mock // Tạo mock object (giả lập)
    private StudentRepository studentRepo;

    @Mock
    private PersonRepository personRepo;

    @Mock
    private MajorRepository majorRepo;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks // Tạo object thật và inject tất cả @Mock vào
    private StudentService studentService;
}
```

| Annotation      | Vai trò                                                        |
|-----------------|----------------------------------------------------------------|
| `@Mock`         | Tạo **giả lập** của interface/class, mọi method trả về default |
| `@InjectMocks`  | Tạo **object thật** và tự động inject các `@Mock` vào          |
| `@ExtendWith`   | Kích hoạt Mockito extension cho JUnit 5                        |

> **Lưu ý**: `@InjectMocks` sẽ inject qua **constructor** (khớp với `@RequiredArgsConstructor` của Lombok)

### 3.3 `when(...).thenReturn(...)` – Giả lập hành vi

```java
import static org.mockito.Mockito.*;

@Test
void shouldReturnStudentWhenIdExists() {
    UUID id = UUID.randomUUID();
    Student mockStudent = Student.builder()
            .studentCode("STU001")
            .enrollmentYear(2024)
            .build();

    StudentDetailResponse mockResponse = new StudentDetailResponse(
        /* ... các field ... */
    );

    // Khi gọi studentRepo.findById(id) → trả về Optional chứa mockStudent
    when(studentRepo.findById(id)).thenReturn(Optional.of(mockStudent));

    // Khi gọi studentMapper.toDetailResponse(mockStudent) → trả về mockResponse
    when(studentMapper.toDetailResponse(mockStudent)).thenReturn(mockResponse);

    // Gọi method cần test
    StudentDetailResponse result = studentService.getById(id);

    // Kiểm tra kết quả
    assertNotNull(result);
    assertEquals(mockResponse, result);
}
```

### 3.4 `verify(...)` – Kiểm tra method có được gọi không

```java
@Test
void shouldCallRepositoryWithCorrectId() {
    UUID id = UUID.randomUUID();
    when(studentRepo.findById(id)).thenReturn(Optional.of(new Student()));
    when(studentMapper.toDetailResponse(any())).thenReturn(mock(StudentDetailResponse.class));

    studentService.getById(id);

    // Verify: studentRepo.findById() được gọi đúng 1 lần với đúng tham số id
    verify(studentRepo, times(1)).findById(id);

    // Verify: studentMapper.toDetailResponse() được gọi đúng 1 lần
    verify(studentMapper, times(1)).toDetailResponse(any(Student.class));

    // Verify: studentRepo.delete() KHÔNG được gọi
    verify(studentRepo, never()).delete(any());
}
```

Các verification mode:

| Mode              | Ý nghĩa                               |
|-------------------|----------------------------------------|
| `times(n)`        | Được gọi đúng **n** lần               |
| `times(1)`        | Được gọi đúng 1 lần (mặc định)        |
| `never()`         | KHÔNG được gọi                         |
| `atLeastOnce()`   | Được gọi ít nhất 1 lần                |
| `atMost(n)`       | Được gọi tối đa n lần                 |

### 3.5 `when(...).thenThrow(...)` – Giả lập exception

```java
@Test
void shouldThrowWhenStudentNotFound() {
    UUID id = UUID.randomUUID();

    // Giả lập: findById trả về empty
    when(studentRepo.findById(id)).thenReturn(Optional.empty());

    // Kỳ vọng: ném ResponseStatusException
    ResponseStatusException ex = assertThrows(
            ResponseStatusException.class,
            () -> studentService.getById(id)
    );

    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    assertTrue(ex.getReason().contains(id.toString()));
}
```

### 3.6 Argument Matchers

Khi **không quan tâm** tham số cụ thể:

```java
import static org.mockito.ArgumentMatchers.*;

// any() – bất kỳ giá trị nào (kể cả null)
when(studentRepo.findById(any())).thenReturn(Optional.empty());

// any(Class) – bất kỳ giá trị nào thuộc type đó
when(studentMapper.toDetailResponse(any(Student.class))).thenReturn(mockResponse);

// anyString() – bất kỳ String nào
when(studentRepo.existsByStudentCode(anyString())).thenReturn(false);

// eq(value) – đúng giá trị cụ thể (dùng khi mix với any())
when(studentRepo.findByEnrollmentYear(eq(2024), any(Pageable.class)))
        .thenReturn(Page.empty());
```

> ⚠️ **Quy tắc quan trọng**: Nếu dùng matcher cho **một** tham số, thì **tất cả** tham số đều phải dùng matcher
>
> ```java
> // ❌ SAI – mix matcher và giá trị thật
> when(repo.findByEnrollmentYear(2024, any())).thenReturn(...);
>
> // ✅ ĐÚNG – dùng eq() cho giá trị cụ thể
> when(repo.findByEnrollmentYear(eq(2024), any())).thenReturn(...);
> ```

### 3.7 `@Spy` – Partial mock

```java
@Spy
private StudentService studentServiceSpy;

@Test
void shouldCallRealMethodExceptMocked() {
    // Spy = object thật, nhưng có thể override từng method
    doReturn(true).when(studentServiceSpy).someHelperMethod();

    // Các method khác vẫn gọi code thật
    studentServiceSpy.anotherMethod();
}
```

> **`@Mock`** = tất cả method trả về default (null, 0, false)
>
> **`@Spy`** = tất cả method chạy code thật, chỉ override method cụ thể

### 3.8 `ArgumentCaptor` – Bắt tham số đã truyền

```java
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

@Captor
private ArgumentCaptor<Student> studentCaptor;

@Test
void shouldSaveStudentWithCorrectData() {
    // ... setup & gọi studentService.create(req) ...

    // Bắt tham số mà studentRepo.saveAndFlush() đã nhận
    verify(studentRepo).saveAndFlush(studentCaptor.capture());

    Student savedStudent = studentCaptor.getValue();
    assertEquals("STU001", savedStudent.getStudentCode());
    assertEquals(2024, savedStudent.getEnrollmentYear());
}
```

---

## 4) Mẫu Unit Test cho Service Layer

### 4.1 Cấu trúc test theo AAA Pattern

Mỗi test case nên tuân theo pattern **AAA** (Arrange – Act – Assert):

```java
@Test
void shouldReturnStudentById() {
    // ===== ARRANGE – Chuẩn bị dữ liệu & mock =====
    UUID id = UUID.randomUUID();
    Student student = Student.builder()
            .studentCode("STU001")
            .enrollmentYear(2024)
            .build();
    StudentDetailResponse expectedResponse = /* ... */;

    when(studentRepo.findById(id)).thenReturn(Optional.of(student));
    when(studentMapper.toDetailResponse(student)).thenReturn(expectedResponse);

    // ===== ACT – Gọi method cần test =====
    StudentDetailResponse actualResponse = studentService.getById(id);

    // ===== ASSERT – Kiểm tra kết quả =====
    assertNotNull(actualResponse);
    assertEquals(expectedResponse, actualResponse);
    verify(studentRepo).findById(id);
}
```

### 4.2 Đặt tên test method

> **Convention**: `should_<KếtQuảMongĐợi>_when_<ĐiềuKiện>`

```java
// ✅ Tên tốt – rõ ràng ý nghĩa
void shouldReturnStudent_whenIdExists()
void shouldThrowNotFound_whenIdDoesNotExist()
void shouldCreateStudent_whenDataIsValid()
void shouldThrowConflict_whenStudentCodeAlreadyExists()
void shouldDeleteStudent_whenIdExists()

// ❌ Tên tệ – không rõ ý nghĩa
void test1()
void testGetById()
void createTest()
```

### 4.3 Test helper: tạo test data factory

Khi nhiều test cần dùng chung dữ liệu → tạo helper method:

```java
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    // ... @Mock, @InjectMocks ...

    // ===== Test Data Factory =====
    private Person createMockPerson() {
        return Person.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .dob(LocalDate.of(2000, 1, 1))
                .phone("0901234567")
                .contactEmail("a@example.com")
                .address("Ha Noi")
                .build();
    }

    private Student createMockStudent() {
        return Student.builder()
                .id(UUID.randomUUID())
                .person(createMockPerson())
                .studentCode("STU001")
                .enrollmentYear(2024)
                .build();
    }

    private PersonCreateRequest createPersonRequest() {
        return new PersonCreateRequest(
                "Nguyen Van A",
                LocalDate.of(2000, 1, 1),
                "0901234567",
                "a@example.com",
                "Ha Noi"
        );
    }

    private StudentCreateOnlyRequest createStudentOnlyRequest() {
        return new StudentCreateOnlyRequest("STU001", 2024, null);
    }

    private StudentCreateRequest createStudentRequest() {
        return new StudentCreateRequest(createPersonRequest(), createStudentOnlyRequest());
    }
}
```

---

## 5) Unit Test Best Practices

### 5.1 Nguyên tắc FIRST

| Chữ cái | Nguyên tắc     | Mô tả                                                   |
|---------|----------------|-----------------------------------------------------------| 
| **F**   | Fast           | Test phải chạy nhanh (mili-giây), không kết nối DB/network |
| **I**   | Independent    | Mỗi test độc lập, không phụ thuộc thứ tự chạy             |
| **R**   | Repeatable     | Chạy bao nhiêu lần cũng cho cùng kết quả                  |
| **S**   | Self-validating| Test tự kiểm tra đúng/sai, không cần check thủ công        |
| **T**   | Timely         | Viết test cùng lúc hoặc trước khi viết code               |

### 5.2 Một test = Một hành vi

```java
// ❌ SAI – Test quá nhiều hành vi trong 1 test
@Test
void testStudentService() {
    // test create
    StudentDetailResponse created = studentService.create(req);
    assertNotNull(created);

    // test getById
    StudentDetailResponse found = studentService.getById(id);
    assertEquals(created, found);

    // test delete
    studentService.deleteById(id);
    assertThrows(Exception.class, () -> studentService.getById(id));
}

// ✅ ĐÚNG – Mỗi test chỉ kiểm tra 1 hành vi
@Test
void shouldCreateStudentSuccessfully() { /* ... */ }

@Test
void shouldReturnStudentWhenIdExists() { /* ... */ }

@Test
void shouldDeleteStudentWhenIdExists() { /* ... */ }
```

### 5.3 Test Happy Path + Edge Cases

```java
@Nested
@DisplayName("getById")
class GetByIdTests {

    @Test
    @DisplayName("Happy path: trả về student khi tìm thấy")
    void happyPath() { /* ... */ }

    @Test
    @DisplayName("Edge case: ném NOT_FOUND khi id không tồn tại")
    void notFound() { /* ... */ }

    @Test
    @DisplayName("Edge case: ném exception khi id là null")
    void nullId() { /* ... */ }
}

@Nested
@DisplayName("create")
class CreateTests {

    @Test
    @DisplayName("Happy path: tạo thành công với dữ liệu hợp lệ")
    void happyPath() { /* ... */ }

    @Test
    @DisplayName("Edge case: ném CONFLICT khi student code đã tồn tại")
    void duplicateCode() { /* ... */ }

    @Test
    @DisplayName("Edge case: ném CONFLICT khi phone đã tồn tại")
    void duplicatePhone() { /* ... */ }

    @Test
    @DisplayName("Edge case: ném BAD_REQUEST khi person là null")
    void nullPerson() { /* ... */ }
}
```

### 5.4 Không test implementation details

```java
// ❌ SAI – Test implementation (phụ thuộc vào cách viết code bên trong)
@Test
void shouldCallNormalizerBeforeSave() {
    // Verify rằng normalizeCode() được gọi → quá chi tiết
    verify(normalizerUtil).normalizeCode(anyString());
}

// ✅ ĐÚNG – Test behavior (kiểm tra kết quả đầu ra)
@Test
void shouldSaveStudentWithNormalizedCode() {
    // Setup: truyền "  stu001  "
    // Assert: student được save với code "STU001" (đã normalize)
    verify(studentRepo).saveAndFlush(studentCaptor.capture());
    assertEquals("STU001", studentCaptor.getValue().getStudentCode());
}
```

### 5.5 Không dùng logic phức tạp trong test

```java
// ❌ SAI – có if/else, loop trong test
@Test
void shouldReturnCorrectStudents() {
    List<Student> students = studentService.getAll();
    for (Student s : students) {
        if (s.getEnrollmentYear() > 2020) {
            assertTrue(s.isActive());
        }
    }
}

// ✅ ĐÚNG – Assertion trực tiếp, rõ ràng
@Test
void shouldReturnActiveStudentsAfter2020() {
    // Arrange: mock cụ thể
    // Act: gọi method
    // Assert: kiểm tra trực tiếp
    assertEquals(3, result.getTotalElements());
}
```

---

## 6) Checklist & Lỗi hay gặp dành cho người mới

### 6.1 Checklist trước khi viết test

- [ ] Xác định **method nào** cần test
- [ ] Liệt kê các **trường hợp** (happy path, edge case, exception)
- [ ] Xác định cần **mock những dependency nào**
- [ ] Mỗi test chỉ test **một hành vi**
- [ ] Đặt tên test method **rõ ràng** (should...when...)
- [ ] Dùng pattern **AAA** (Arrange – Act – Assert)

### 6.2 Top 15 lỗi thường gặp

#### ❌ Lỗi 1: Quên `@ExtendWith(MockitoExtension.class)`

```java
// ❌ SAI – @Mock không hoạt động → NullPointerException
class StudentServiceTest {
    @Mock
    private StudentRepository studentRepo; // → null!

    @InjectMocks
    private StudentService studentService; // → null!
}

// ✅ ĐÚNG
@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository studentRepo; // → mock object

    @InjectMocks
    private StudentService studentService; // → real object với mock inject
}
```

#### ❌ Lỗi 2: Mock trả về `null` vì quên setup `when()`

```java
// ❌ SAI – Không setup when() → findById() trả về null (default)
@Test
void test() {
    StudentDetailResponse result = studentService.getById(id);
    // → NullPointerException vì findById() trả về null, không phải Optional
}

// ✅ ĐÚNG – Setup when() trước khi gọi
@Test
void test() {
    when(studentRepo.findById(id)).thenReturn(Optional.of(student));
    StudentDetailResponse result = studentService.getById(id);
}
```

#### ❌ Lỗi 3: Nhầm `@Mock` và `@InjectMocks`

```java
// ❌ SAI – Service bị mock → gọi method trả về null thay vì chạy code thật
@Mock
private StudentService studentService; // ← SAI! Service cần test phải dùng @InjectMocks

// ✅ ĐÚNG
@InjectMocks
private StudentService studentService; // ← Object thật, chạy code thật
```

#### ❌ Lỗi 4: Không mock đủ dependency

```java
// ❌ SAI – Quên mock EntityManager → NullPointerException khi service gọi entityManager.refresh()
@Mock
private StudentRepository studentRepo;
// thiếu: @Mock EntityManager entityManager

@InjectMocks
private StudentService studentService;

// ✅ ĐÚNG – Mock TẤT CẢ dependency
@Mock private StudentRepository studentRepo;
@Mock private PersonRepository personRepo;
@Mock private MajorRepository majorRepo;
@Mock private StudentMapper studentMapper;
@Mock private EntityManager entityManager;

@InjectMocks
private StudentService studentService;
```

#### ❌ Lỗi 5: Dùng `assertThrows` sai cách

```java
// ❌ SAI – Gọi method NGOÀI lambda
@Test
void test() {
    studentService.getById(nonExistentId); // Exception ném TRƯỚC assertThrows!
    assertThrows(ResponseStatusException.class, () -> {});
}

// ✅ ĐÚNG – Gọi method BÊN TRONG lambda
@Test
void test() {
    assertThrows(ResponseStatusException.class,
        () -> studentService.getById(nonExistentId));
}
```

#### ❌ Lỗi 6: So sánh object bằng `assertEquals` mà chưa override `equals()`

```java
// ❌ SAI – Student chưa override equals() → assertEquals so sánh reference
assertEquals(expectedStudent, actualStudent); // → FAIL dù data giống nhau

// ✅ ĐÚNG – So sánh từng field hoặc dùng assertAll
assertAll(
    () -> assertEquals(expected.getStudentCode(), actual.getStudentCode()),
    () -> assertEquals(expected.getEnrollmentYear(), actual.getEnrollmentYear())
);
```

#### ❌ Lỗi 7: Test phụ thuộc vào thứ tự chạy

```java
// ❌ SAI – Test 2 phụ thuộc kết quả test 1
private static UUID createdId;

@Test @Order(1)
void shouldCreate() { createdId = service.create(req).getId(); }

@Test @Order(2)
void shouldFindCreated() { service.getById(createdId); } // phụ thuộc test 1!

// ✅ ĐÚNG – Mỗi test tự setup dữ liệu riêng
@Test
void shouldFindCreated() {
    UUID id = UUID.randomUUID();
    when(repo.findById(id)).thenReturn(Optional.of(student));
    // ...
}
```

#### ❌ Lỗi 8: Mix argument matcher với giá trị thật

```java
// ❌ SAI – compiler/runtime error
when(repo.findByEnrollmentYear(2024, any())).thenReturn(page);

// ✅ ĐÚNG – dùng eq() cho giá trị cụ thể
when(repo.findByEnrollmentYear(eq(2024), any())).thenReturn(page);
```

#### ❌ Lỗi 9: Quên `@Test` annotation

```java
// ❌ SAI – Method này sẽ KHÔNG được chạy!
void shouldReturnStudent() { /* ... */ }

// ✅ ĐÚNG
@Test
void shouldReturnStudent() { /* ... */ }
```

#### ❌ Lỗi 10: Import sai JUnit version

```java
// ❌ SAI – JUnit 4 (cũ)
import org.junit.Test;
import org.junit.Before;

// ✅ ĐÚNG – JUnit 5 (Jupiter)
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
```

#### ❌ Lỗi 11: Test quá nhiều thứ trong 1 test method

```java
// ❌ SAI – Test cả create, get, update, delete
@Test
void testCRUD() {
    service.create(req);
    service.getById(id);
    service.patch(id, patchReq);
    service.deleteById(id);
}

// ✅ ĐÚNG – Mỗi test 1 hành vi
@Test void shouldCreateSuccessfully() { /* ... */ }
@Test void shouldGetByIdSuccessfully() { /* ... */ }
@Test void shouldPatchSuccessfully() { /* ... */ }
@Test void shouldDeleteSuccessfully() { /* ... */ }
```

#### ❌ Lỗi 12: Không assert gì cả

```java
// ❌ SAI – Chạy không lỗi ≠ đúng
@Test
void shouldCreateStudent() {
    studentService.create(req);
    // Không có assertion! Test luôn pass dù logic sai
}

// ✅ ĐÚNG – Luôn có assertion
@Test
void shouldCreateStudent() {
    StudentDetailResponse result = studentService.create(req);
    assertNotNull(result);
    verify(studentRepo).saveAndFlush(any(Student.class));
}
```

#### ❌ Lỗi 13: Test private method trực tiếp

```java
// ❌ SAI – Cố gắng test private method
@Test
void testValidateStudentCode() {
    // Reflection hack để gọi private method → bẫy!
    Method method = StudentService.class.getDeclaredMethod("validateStudentCode", String.class);
    method.setAccessible(true);
    method.invoke(studentService, "STU001");
}

// ✅ ĐÚNG – Test thông qua public method (gián tiếp test private)
@Test
void shouldThrowWhenStudentCodeIsNull() {
    StudentCreateRequest req = new StudentCreateRequest(
            createPersonRequest(),
            new StudentCreateOnlyRequest(null, 2024, null) // studentCode = null
    );
    assertThrows(ResponseStatusException.class,
            () -> studentService.create(req));
}
```

#### ❌ Lỗi 14: Dùng `doReturn` sai ngữ cảnh

```java
// ❌ SAI – doReturn dùng cho @Spy, không cần cho @Mock thông thường
doReturn(Optional.of(student)).when(studentRepo).findById(id);

// ✅ ĐÚNG – Với @Mock, dùng when().thenReturn() là đủ
when(studentRepo.findById(id)).thenReturn(Optional.of(student));

// Khi nào dùng doReturn?
// → Khi mock @Spy, hoặc khi method trả về void
doNothing().when(studentRepo).delete(any());
```

#### ❌ Lỗi 15: Hardcode UUID trong test

```java
// ❌ SAI – UUID hardcode khó maintain, khó đọc
@Test
void test() {
    UUID id = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
}

// ✅ ĐÚNG – Generate random
@Test
void test() {
    UUID id = UUID.randomUUID();
}
```

### 6.3 Quick Reference – Cheat Sheet

| Tình huống                    | Code                                                    |
|-------------------------------|---------------------------------------------------------|
| Mock method trả về giá trị    | `when(mock.method()).thenReturn(value)`                  |
| Mock method ném exception     | `when(mock.method()).thenThrow(new Ex())`                |
| Mock void method              | `doNothing().when(mock).voidMethod()`                    |
| Verify method được gọi        | `verify(mock).method()`                                 |
| Verify KHÔNG được gọi         | `verify(mock, never()).method()`                         |
| Bắt tham số                  | `verify(mock).method(captor.capture())`                  |
| Bất kỳ tham số nào           | `when(mock.method(any())).thenReturn(value)`             |
| Kiểm tra exception           | `assertThrows(Ex.class, () -> call())`                   |
| Kiểm tra nhiều assertion      | `assertAll(() -> assert1(), () -> assert2())`            |

---

## 7) Bài tập: Viết Unit Test cho StudentService

### 7.1 Yêu cầu

Tạo file `src/test/java/student/management/api_app/service/impl/StudentServiceTest.java` và viết unit test cho **`StudentService`**.

### 7.2 Chuẩn bị

#### Bước 1: Tạo package test

Tạo cấu trúc thư mục:

```
src/test/java/student/management/api_app/service/impl/
```

#### Bước 2: Tạo skeleton test class

```java
package student.management.api_app.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.person.PersonCreateRequest;
import student.management.api_app.dto.student.*;
import student.management.api_app.mapper.StudentMapper;
import student.management.api_app.model.Person;
import student.management.api_app.model.Student;
import student.management.api_app.repository.MajorRepository;
import student.management.api_app.repository.PersonRepository;
import student.management.api_app.repository.StudentRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentService Unit Tests")
class StudentServiceTest {

    @Mock private StudentRepository studentRepo;
    @Mock private PersonRepository personRepo;
    @Mock private MajorRepository majorRepo;
    @Mock private StudentMapper studentMapper;
    @Mock private EntityManager entityManager;

    @InjectMocks
    private StudentService studentService;

    // ===== Test Data Factory =====

    private Person createMockPerson() {
        return Person.builder()
                .id(UUID.randomUUID())
                .fullName("Nguyen Van A")
                .dob(LocalDate.of(2000, 1, 1))
                .phone("0901234567")
                .contactEmail("a@example.com")
                .address("Ha Noi")
                .build();
    }

    private Student createMockStudent() {
        return Student.builder()
                .id(UUID.randomUUID())
                .person(createMockPerson())
                .studentCode("STU001")
                .enrollmentYear(2024)
                .build();
    }

    private StudentDetailResponse createMockDetailResponse() {
        // Tạo response phù hợp với cấu trúc project
        return mock(StudentDetailResponse.class);
    }

    private PersonCreateRequest createPersonRequest() {
        return new PersonCreateRequest(
                "Nguyen Van A",
                LocalDate.of(2000, 1, 1),
                "0901234567",
                "a@example.com",
                "Ha Noi"
        );
    }

    private StudentCreateOnlyRequest createStudentOnlyRequest() {
        return new StudentCreateOnlyRequest("STU001", 2024, null);
    }

    private StudentCreateRequest createStudentRequest() {
        return new StudentCreateRequest(createPersonRequest(), createStudentOnlyRequest());
    }

    // ===== TESTS =====

    // TODO: Sinh viên implement các test case bên dưới
}
```

### 7.3 Các test case cần viết

#### Nhóm 1: `getById` (3 test)

```java
@Nested
@DisplayName("getById")
class GetByIdTests {

    @Test
    @DisplayName("TC1: Trả về StudentDetailResponse khi id tồn tại")
    void should_ReturnStudentDetail_when_IdExists() {
        // Arrange
        UUID id = UUID.randomUUID();
        Student student = createMockStudent();
        StudentDetailResponse expectedResponse = createMockDetailResponse();

        when(studentRepo.findById(id)).thenReturn(Optional.of(student));
        when(studentMapper.toDetailResponse(student)).thenReturn(expectedResponse);

        // Act
        StudentDetailResponse result = studentService.getById(id);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(studentRepo, times(1)).findById(id);
        verify(studentMapper, times(1)).toDetailResponse(student);
    }

    @Test
    @DisplayName("TC2: Ném NOT_FOUND khi id không tồn tại")
    void should_ThrowNotFound_when_IdDoesNotExist() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - when(studentRepo.findById(id)).thenReturn(Optional.empty())
        // - assertThrows ResponseStatusException
        // - Kiểm tra status code = 404
    }

    @Test
    @DisplayName("TC3: Message chứa id khi không tìm thấy")
    void should_ContainIdInMessage_when_NotFound() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - Bắt exception bằng assertThrows
        // - assertTrue(ex.getReason().contains(id.toString()))
    }
}
```

#### Nhóm 2: `getByStudentCode` (2 test)

```java
@Nested
@DisplayName("getByStudentCode")
class GetByStudentCodeTests {

    @Test
    @DisplayName("TC4: Trả về student khi code tồn tại")
    void should_ReturnStudent_when_CodeExists() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - when(studentRepo.findByStudentCode("STU001")).thenReturn(Optional.of(student))
        // - when(studentMapper.toDetailResponse(student)).thenReturn(expectedResponse)
        // - Gọi studentService.getByStudentCode("STU001")
    }

    @Test
    @DisplayName("TC5: Ném BAD_REQUEST khi code là null")
    void should_ThrowBadRequest_when_CodeIsNull() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - Gọi studentService.getByStudentCode(null)
        // - assertThrows, kiểm tra status code = 400
    }
}
```

#### Nhóm 3: `create` (4 test)

```java
@Nested
@DisplayName("create")
class CreateTests {

    @Test
    @DisplayName("TC6: Tạo student thành công với dữ liệu hợp lệ")
    void should_CreateStudent_when_DataIsValid() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - Mock personRepo.existsByPhone() → false
        // - Mock studentRepo.existsByStudentCode() → false
        // - Mock personRepo.saveAndFlush() (doNothing hoặc trả về person)
        // - Mock studentRepo.saveAndFlush() (doNothing hoặc trả về student)
        // - Mock entityManager.refresh() → doNothing
        // - Mock studentMapper.toDetailResponse() → expected response
        // - Verify personRepo.saveAndFlush() được gọi 1 lần
        // - Verify studentRepo.saveAndFlush() được gọi 1 lần
    }

    @Test
    @DisplayName("TC7: Ném BAD_REQUEST khi person là null")
    void should_ThrowBadRequest_when_PersonIsNull() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - StudentCreateRequest req = new StudentCreateRequest(null, createStudentOnlyRequest())
        // - assertThrows ResponseStatusException, status = 400
    }

    @Test
    @DisplayName("TC8: Ném CONFLICT khi phone đã tồn tại")
    void should_ThrowConflict_when_PhoneExists() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - when(personRepo.existsByPhone("0901234567")).thenReturn(true)
        // - assertThrows ResponseStatusException, status = 409
    }

    @Test
    @DisplayName("TC9: Ném CONFLICT khi student code đã tồn tại")
    void should_ThrowConflict_when_StudentCodeExists() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - Mock personRepo.existsByPhone() → false (phone OK)
        // - Mock personRepo.saveAndFlush() → OK
        // - when(studentRepo.existsByStudentCode("STU001")).thenReturn(true)
        // - assertThrows ResponseStatusException, status = 409
    }
}
```

#### Nhóm 4: `deleteById` (2 test)

```java
@Nested
@DisplayName("deleteById")
class DeleteByIdTests {

    @Test
    @DisplayName("TC10: Xóa thành công khi id tồn tại")
    void should_DeleteStudent_when_IdExists() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - when(studentRepo.findById(id)).thenReturn(Optional.of(student))
        // - Gọi studentService.deleteById(id)
        // - verify(studentRepo).delete(student)
    }

    @Test
    @DisplayName("TC11: Ném NOT_FOUND khi id không tồn tại")
    void should_ThrowNotFound_when_IdDoesNotExist() {
        // TODO: Sinh viên tự implement
    }
}
```

#### Nhóm 5: `listByMajorId` (2 test)

```java
@Nested
@DisplayName("listByMajorId")
class ListByMajorIdTests {

    @Test
    @DisplayName("TC12: Ném NOT_FOUND khi major id không tồn tại")
    void should_ThrowNotFound_when_MajorIdNotExists() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - when(majorRepo.existsById(majorId)).thenReturn(false)
        // - assertThrows ResponseStatusException, status = 404
    }

    @Test
    @DisplayName("TC13: Trả về danh sách student khi major id tồn tại")
    void should_ReturnStudents_when_MajorIdExists() {
        // TODO: Sinh viên tự implement
        // Gợi ý:
        // - when(majorRepo.existsById(majorId)).thenReturn(true)
        // - when(studentRepo.findByMajor_Id(...)).thenReturn(Page.empty())
        // - Kiểm tra result không null
    }
}
```

### 7.4 Chạy test

```bash
# Chạy tất cả test
./gradlew test

# Chạy test trong 1 class cụ thể
./gradlew test --tests "student.management.api_app.service.impl.StudentServiceTest"

# Chạy 1 test method cụ thể
./gradlew test --tests "student.management.api_app.service.impl.StudentServiceTest.GetByIdTests.should_ReturnStudentDetail_when_IdExists"

# Xem report (sau khi chạy test)
# Mở file: build/reports/tests/test/index.html
```

### 7.5 Checklist bài nộp

- [ ] File test nằm đúng package: `src/test/java/student/management/api_app/service/impl/StudentServiceTest.java`
- [ ] Có `@ExtendWith(MockitoExtension.class)`
- [ ] Mock đủ 5 dependency: `StudentRepository`, `PersonRepository`, `MajorRepository`, `StudentMapper`, `EntityManager`
- [ ] Có `@InjectMocks` trên `StudentService`
- [ ] Tối thiểu **13 test case** (TC1 → TC13)
- [ ] Mỗi test có đầy đủ **Arrange – Act – Assert**
- [ ] Tất cả test **pass** khi chạy `./gradlew test`
- [ ] Tên test method theo convention: `should_..._when_...`
- [ ] Sử dụng `@Nested` và `@DisplayName` cho mỗi nhóm test

---

## 8) Tổng kết

| Nội dung              | Kiến thức chính                                              |
|-----------------------|--------------------------------------------------------------|
| JUnit 5               | `@Test`, `@BeforeEach`, `@Nested`, `@ParameterizedTest`, `@DisplayName` |
| Assertions            | `assertEquals`, `assertThrows`, `assertAll`, `assertNotNull` |
| Mockito               | `@Mock`, `@InjectMocks`, `when().thenReturn()`, `verify()`  |
| Argument Matchers     | `any()`, `eq()`, `anyString()`, `ArgumentCaptor`             |
| Best Practices        | AAA Pattern, FIRST principles, test behavior not implementation |
| Common Mistakes       | 15 lỗi phổ biến nhất cho người mới bắt đầu JUnit            |
