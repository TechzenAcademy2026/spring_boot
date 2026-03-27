# Spring Boot – Buổi 9: Validation, Exception Handling & Logging

## 1) Bean Validation (JSR-380)

### 1.1 Khái niệm

> **Bean Validation** là chuẩn Java (JSR-380) cho phép khai báo ràng buộc dữ liệu bằng annotation trực tiếp trên field hoặc parameter.
>
> Spring Boot tích hợp sẵn qua dependency `spring-boot-starter-validation` (Hibernate Validator là implementation mặc định).

### 1.2 Dependency

* Project đã có sẵn trong `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-validation'
}
```

### 1.3 Các annotation validation phổ biến

| Annotation                          | Mục đích                                          | Ví dụ                                          |
|-------------------------------------|---------------------------------------------------|-------------------------------------------------|
| `@NotNull`                          | Không được `null`                                 | `@NotNull UUID id`                              |
| `@NotEmpty`                         | Không `null` và không rỗng (String, Collection)   | `@NotEmpty String name`                         |
| `@NotBlank`                         | Không `null`, không rỗng, không toàn khoảng trắng | `@NotBlank String fullName`                     |
| `@Size(min, max)`                   | Ràng buộc độ dài                                  | `@Size(min = 2, max = 150) String fullName`     |
| `@Min(value)` / `@Max(value)`      | Giá trị số tối thiểu / tối đa                    | `@Min(1900) Integer enrollmentYear`             |
| `@Email`                            | Phải đúng format email                            | `@Email String contactEmail`                    |
| `@Pattern(regexp)`                  | Phải khớp regex                                   | `@Pattern(regexp = "^[0-9]{10,11}$") String phone` |
| `@Past` / `@PastOrPresent`         | Ngày trong quá khứ                                | `@Past LocalDate dob`                           |
| `@Future` / `@FutureOrPresent`     | Ngày trong tương lai                              | `@Future LocalDate deadline`                    |
| `@Positive` / `@PositiveOrZero`    | Số dương / không âm                               | `@Positive Integer credit`                      |

### 1.4 Phân biệt `@NotNull`, `@NotEmpty`, `@NotBlank`

| Giá trị       | `@NotNull` | `@NotEmpty` | `@NotBlank` |
|----------------|:----------:|:-----------:|:-----------:|
| `null`         | ❌         | ❌          | ❌          |
| `""`           | ✅         | ❌          | ❌          |
| `"   "`        | ✅         | ✅          | ❌          |
| `"Nguyen A"`   | ✅         | ✅          | ✅          |

---

## 2) Áp dụng Validation vào DTO

### 2.1 Thêm annotation validation vào `PersonCreateRequest`

```java
// student/management/api_app/dto/person/PersonCreateRequest.java

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PersonCreateRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @Past(message = "Date of birth must be in the past")
        LocalDate dob,

        @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone must be 10-11 digits")
        String phone,

        @Email(message = "Contact email must be valid")
        @Size(max = 200, message = "Email must not exceed 200 characters")
        String contactEmail,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
```

### 2.2 Thêm annotation validation vào `StudentCreateOnlyRequest`

```java
// student/management/api_app/dto/student/StudentCreateOnlyRequest.java

import jakarta.validation.constraints.*;
import java.util.UUID;

public record StudentCreateOnlyRequest(
        @NotBlank(message = "Student code is required")
        @Size(max = 50, message = "Student code must not exceed 50 characters")
        String studentCode,

        @NotNull(message = "Enrollment year is required")
        @Min(value = 1900, message = "Enrollment year must be >= 1900")
        Integer enrollmentYear,

        UUID majorId
) {
}
```

### 2.3 Validate nested object với `@Valid`

Khi DTO chứa DTO con (nested), cần đánh dấu `@Valid` trên field con để Spring validate đệ quy:

```java
// student/management/api_app/dto/student/StudentCreateRequest.java

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import student.management.api_app.dto.person.PersonCreateRequest;

public record StudentCreateRequest(
        @Valid  // Validate đệ quy vào PersonCreateRequest
        @NotNull(message = "Person info is required")
        PersonCreateRequest person,

        @Valid  // Validate đệ quy vào StudentCreateOnlyRequest
        @NotNull(message = "Student info is required")
        StudentCreateOnlyRequest student
) {
}
```

> **Lưu ý**: Nếu KHÔNG đặt `@Valid` thì Spring sẽ **bỏ qua** toàn bộ annotation bên trong `PersonCreateRequest` và `StudentCreateOnlyRequest` → dữ liệu invalid vẫn vượt qua validation

### 2.4 Kích hoạt Validation trên Controller

Thêm `@Valid` trước `@RequestBody` để kích hoạt validation:

```java
// student/management/api_app/controller/student/StudentController.java

@PostMapping
public ResponseEntity<AppResponse<StudentDetailResponse>> create(
        @Valid @RequestBody StudentCreateRequest req) {
    // Nếu validation fail → Spring tự động ném MethodArgumentNotValidException
    // Nếu validation pass → code tiếp tục chạy bình thường
    StudentDetailResponse created = service.create(req);
    // ...
}
```

> **Luồng hoạt động:**
> 1. Client gửi POST request với body JSON
> 2. Spring deserialize JSON thành `StudentCreateRequest`
> 3. Gặp `@Valid` → Spring Boot (Hibernate Validator) kiểm tra tất cả annotation validation
> 4. Nếu có lỗi → ném `MethodArgumentNotValidException` (HTTP 400) **trước khi** vào method body
> 5. Nếu hợp lệ → code tiếp tục chạy

### 2.5 Validate `@RequestParam` và `@PathVariable` với `@Validated`

Các annotation constraint (`@Min`, `@Max`, `@Pattern`...) đặt trực tiếp trên `@RequestParam` hoặc `@PathVariable` mặc định **không được kích hoạt** nếu chỉ dùng `@Valid @RequestBody`.

Cần thêm `@Validated` của Spring lên class controller để bật tính năng này:

```java
import org.springframework.validation.annotation.Validated;

@Validated // Spring kích hoạt validation cho @RequestParam, @PathVariable
@RestController
@RequestMapping("${api.prefix}/students")
public class StudentController {

    @GetMapping("/by-year")
    public ResponseEntity<?> listByEnrollmentYear(
            @RequestParam("year")
            @Min(value = 1900, message = "Year must be >= 1900")
            @Max(value = 2100, message = "Year must be <= 2100")
            Integer year,
            @PageableDefault(size = 5) Pageable pageable) {
        // ...
    }
}
```

> Khi validation fail ở `@RequestParam` / `@PathVariable`, Spring ném `ConstraintViolationException` (khác với `MethodArgumentNotValidException`)
> → Cần bắt thêm `ConstraintViolationException` trong `GlobalExceptionHandler` nếu muốn tùy chỉnh response

---

## 3) Custom Validation

### 3.1 Khi nào cần Custom Validation?

> * Khi các annotation có sẵn (`@NotBlank`, `@Email`, `@Pattern`...) không đáp ứng được business rule phức tạp
> * Ví dụ: kiểm tra tuổi ≥ 16 từ ngày sinh, kiểm tra mã sinh viên phải bắt đầu bằng "STU", kiểm tra ngày bắt đầu phải trước ngày kết thúc

### 3.2 Tạo Custom Annotation `@AdultAge`

Ràng buộc: ngày sinh phải đảm bảo **đủ 16 tuổi trở lên**

#### Bước 1: Khai báo annotation

```java
// student/management/api_app/validation/AdultAge.java

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AdultAgeValidator.class) // Chỉ định Validator class
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface AdultAge {
    String message() default "Must be at least 16 years old";
    int minAge() default 16;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

| Thành phần                        | Vai trò                                                  |
|-----------------------------------|----------------------------------------------------------|
| `@Constraint(validatedBy = ...)`  | Liên kết annotation với class validator                  |
| `@Target`                         | Nơi được phép đặt annotation (FIELD, PARAMETER, ...)    |
| `@Retention(RUNTIME)`             | Annotation tồn tại lúc runtime để Hibernate Validator đọc |
| `message()`                       | Message mặc định khi validation fail                     |
| `minAge()`                        | Tham số tùy chỉnh (có thể thêm nhiều tham số khác)      |
| `groups()`, `payload()`           | **Bắt buộc** phải có theo chuẩn JSR-380                  |

#### Bước 2: Viết Validator class

```java
// student/management/api_app/validation/AdultAgeValidator.java

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class AdultAgeValidator implements ConstraintValidator<AdultAge, LocalDate> {
    private int minAge;

    @Override
    public void initialize(AdultAge annotation) {
        this.minAge = annotation.minAge();
    }

    @Override
    public boolean isValid(LocalDate dob, ConstraintValidatorContext context) {
        // null check: nếu dob null thì không thuộc trách nhiệm của @AdultAge
        // → để @NotNull hoặc @Past xử lý riêng
        if (dob == null) return true;

        return Period.between(dob, LocalDate.now()).getYears() >= minAge;
    }
}
```

> **Lưu ý quan trọng**: Nếu `dob == null` → trả về `true` (hợp lệ)
> * Đây là convention chuẩn của Bean Validation: **mỗi annotation chỉ chịu trách nhiệm 1 ràng buộc**
> * Nếu muốn bắt buộc `dob` không null → dùng thêm `@NotNull` trước `@AdultAge`

#### Bước 3: Sử dụng annotation

```java
// student/management/api_app/dto/person/PersonCreateRequest.java

import student.management.api_app.validation.AdultAge;

public record PersonCreateRequest(
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        @AdultAge(message = "Student must be at least 16 years old")
        LocalDate dob,

        @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone must be 10-11 digits")
        String phone,

        @Email(message = "Contact email must be valid")
        String contactEmail,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address
) {
}
```

### 3.3 Tạo Custom Annotation `@StudentCodeFormat`

Ràng buộc: mã sinh viên phải bắt đầu bằng **"STU"** + theo sau bởi 3 chữ số

#### Bước 1: Khai báo annotation

```java
// student/management/api_app/validation/StudentCodeFormat.java

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StudentCodeFormatValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface StudentCodeFormat {
    String message() default "Student code must start with 'STU' followed by 3 digits (e.g. STU001)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

#### Bước 2: Viết Validator class

```java
// student/management/api_app/validation/StudentCodeFormatValidator.java

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StudentCodeFormatValidator implements ConstraintValidator<StudentCodeFormat, String> {

    private static final String STUDENT_CODE_PATTERN = "^STU\\d{3}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // để @NotBlank xử lý null
        return value.matches(STUDENT_CODE_PATTERN);
    }
}
```

#### Bước 3: Sử dụng annotation

```java
// student/management/api_app/dto/student/StudentCreateOnlyRequest.java

import student.management.api_app.validation.StudentCodeFormat;

public record StudentCreateOnlyRequest(
        @NotBlank(message = "Student code is required")
        @StudentCodeFormat
        String studentCode,

        @NotNull(message = "Enrollment year is required")
        @Min(value = 1900, message = "Enrollment year must be >= 1900")
        Integer enrollmentYear,

        UUID majorId
) {
}
```

---

## 4) Exception Handling với `@ExceptionHandler`

### 4.1 Vấn đề khi không có Exception Handler

Khi validation fail, Spring ném `MethodArgumentNotValidException` và trả về response dạng:

```json
{
  "timestamp": "2025-01-01T10:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "path": "/api/v1/students"
}
```

> * Response mặc định thiếu thông tin chi tiết về lỗi validation
> * Không thống nhất với format `AppResponse` của project
> * Client không biết field nào sai, lỗi gì

### 4.2 `@ExceptionHandler` cơ bản

`@ExceptionHandler` cho phép bắt exception cụ thể và tuỳ chỉnh response trả về:

```java
@RestController
@RequestMapping("${api.prefix}/students")
public class StudentController {

    // ... các API method ...

    // Bắt MethodArgumentNotValidException chỉ trong controller này
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        // Xử lý lỗi ...
    }
}
```

> `@ExceptionHandler` đặt trong controller chỉ bắt exception xảy ra trong controller đó

### 4.3 `@RestControllerAdvice` – Global Exception Handler

Để xử lý exception **toàn cục** cho tất cả controller → dùng `@RestControllerAdvice`:

```java
// student/management/api_app/exception/GlobalExceptionHandler.java

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.AppResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1) Bắt lỗi Validation (MethodArgumentNotValidException)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(
                AppResponse.<Map<String, String>>builder()
                        .success(false)
                        .data(fieldErrors)
                        .error(AppResponse.AppError.builder()
                                .code("VALIDATION_FAILED")
                                .message("Input validation failed")
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }

    // 2) Bắt ResponseStatusException (ném từ Service)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<AppResponse<Void>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

        return ResponseEntity.status(status).body(
                AppResponse.<Void>builder()
                        .success(false)
                        .error(AppResponse.AppError.builder()
                                .code(status.name())
                                .message(ex.getReason())
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }

    // 3) Bắt mọi exception không mong đợi (fallback)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<Void>> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AppResponse.<Void>builder()
                        .success(false)
                        .error(AppResponse.AppError.builder()
                                .code("INTERNAL_ERROR")
                                .message("An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }
}
```

### 4.4 Kết quả response khi validation fail

Request:

```json
POST /api/v1/students
{
  "person": {
    "fullName": "",
    "dob": "2020-01-01",
    "phone": "abc",
    "contactEmail": "invalid"
  },
  "student": {
    "studentCode": "",
    "enrollmentYear": null
  }
}
```

Response (400 Bad Request):

```json
{
  "success": false,
  "data": {
    "person.fullName": "Full name is required",
    "person.dob": "Student must be at least 16 years old",
    "person.phone": "Phone must be 10-11 digits",
    "person.contactEmail": "Contact email must be valid",
    "student.studentCode": "Student code is required",
    "student.enrollmentYear": "Enrollment year is required"
  },
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Input validation failed",
    "path": "/api/v1/students"
  },
  "timestamp": "2025-01-01T10:00:00Z"
}
```

> **Lưu ý**: field name của nested DTO sẽ có dạng `person.fullName`, `student.studentCode` → dễ phân biệt lỗi thuộc object nào

### 4.5 Tạo Custom Exception

Thay vì dùng `ResponseStatusException`, ta có thể tạo exception riêng:

```java
// student/management/api_app/exception/ResourceNotFoundException.java

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // Mặc định trả về 404
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
```

Sử dụng trong Service:

```java
// student/management/api_app/service/impl/StudentService.java

public StudentDetailResponse getById(UUID id) {
    Student student = studentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
    // ...
}
```

Thêm handler trong `GlobalExceptionHandler`:

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<AppResponse<Void>> handleNotFound(
        ResourceNotFoundException ex,
        HttpServletRequest request) {

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            AppResponse.<Void>builder()
                    .success(false)
                    .error(AppResponse.AppError.builder()
                            .code("NOT_FOUND")
                            .message(ex.getMessage())
                            .path(request.getRequestURI())
                            .build())
                    .build()
    );
}
```

### 4.6 Thứ tự ưu tiên khi Spring xử lý Exception

> Spring chọn `@ExceptionHandler` **cụ thể nhất** (specific) trước:
>
> 1. `ResourceNotFoundException` → handler cho `ResourceNotFoundException`
> 2. `MethodArgumentNotValidException` → handler cho `MethodArgumentNotValidException`
> 3. Nếu không có handler cụ thể, Spring tìm handler cho class cha (parent class)
> 4. `Exception.class` → handler fallback (bắt mọi exception còn lại)

---

## 5) Logging với SLF4J & Logback

### 5.1 Tổng quan Logging trong Spring Boot

> * **SLF4J** (Simple Logging Facade for Java): là **interface** (facade) cho logging, không phải implementation
> * **Logback**: là **implementation** mặc định của SLF4J trong Spring Boot
> * Spring Boot đã tích hợp sẵn cả SLF4J + Logback qua `spring-boot-starter-web` → **không cần thêm dependency**

Kiến trúc:

```
Code → SLF4J (API facade) → Logback (implementation) → Console / File
```

### 5.2 Các level logging

| Level   | Mô tả                                        | Dùng khi                                    |
|---------|-----------------------------------------------|----------------------------------------------|
| `TRACE` | Chi tiết nhất, từng bước nhỏ                  | Debug cực kỳ chi tiết, hiếm dùng            |
| `DEBUG` | Thông tin debug cho developer                 | Development, debug flow                      |
| `INFO`  | Thông tin hoạt động bình thường               | Startup, request xử lý xong, milestone      |
| `WARN`  | Cảnh báo, có thể gây lỗi                     | Config sai, dùng API deprecated              |
| `ERROR` | Lỗi xảy ra, cần xử lý                        | Exception, DB fail, service down             |

> Thứ tự: `TRACE` < `DEBUG` < `INFO` < `WARN` < `ERROR`
>
> Nếu đặt level = `INFO`, thì `TRACE` và `DEBUG` sẽ bị ẩn; chỉ `INFO`, `WARN`, `ERROR` được hiển thị

### 5.3 Sử dụng Logger với `@Slf4j` (Lombok)

Thay vì khai báo thủ công:

```java
private static final Logger log = LoggerFactory.getLogger(StudentService.class);
```

Dùng Lombok `@Slf4j`:

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class StudentService implements IStudentService {

    public StudentDetailResponse getById(UUID id) {
        log.info("Getting student by id: {}", id);

        Student student = studentRepo.findById(id)
                .orElseThrow(() -> {
                    log.warn("Student not found with id: {}", id);
                    return new ResourceNotFoundException("Student", "id", id);
                });

        log.debug("Found student: code={}, name={}",
                student.getStudentCode(),
                student.getPerson().getFullName());

        return studentMapper.toDetailResponse(student);
    }

    @Transactional
    public StudentDetailResponse create(StudentCreateRequest req) {
        log.info("Creating student with code: {}", req.student().studentCode());

        try {
            // ... logic tạo student ...

            log.info("Student created successfully: id={}", savedStudent.getId());
            return studentMapper.toDetailResponse(savedStudent);

        } catch (Exception e) {
            log.error("Failed to create student: code={}", req.student().studentCode(), e);
            throw e;
        }
    }
}
```

#### Giải thích cú pháp `{}`

> * `log.info("Getting student by id: {}", id)` → SLF4J thay `{}` bằng giá trị `id`
> * Dùng `{}` thay vì nối chuỗi (`"... " + id`) → hiệu quả hơn vì SLF4J chỉ format string khi log level được bật
> * Tham số cuối cùng là `Exception` sẽ được in stack trace: `log.error("Failed", e)`

### 5.4 Logging trong `GlobalExceptionHandler`

```java
// student/management/api_app/exception/GlobalExceptionHandler.java

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AppResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        log.warn("Validation failed for [{}]: {}", request.getRequestURI(), fieldErrors);

        return ResponseEntity.badRequest().body(
                AppResponse.<Map<String, String>>builder()
                        .success(false)
                        .data(fieldErrors)
                        .error(AppResponse.AppError.builder()
                                .code("VALIDATION_FAILED")
                                .message("Input validation failed")
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AppResponse<Void>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        log.warn("Resource not found: [{}] {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                AppResponse.<Void>builder()
                        .success(false)
                        .error(AppResponse.AppError.builder()
                                .code("NOT_FOUND")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AppResponse<Void>> handleGeneral(
            Exception ex,
            HttpServletRequest request) {

        // Dùng log.error kèm exception object → in stack trace đầy đủ
        log.error("Unexpected error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                AppResponse.<Void>builder()
                        .success(false)
                        .error(AppResponse.AppError.builder()
                                .code("INTERNAL_ERROR")
                                .message("An unexpected error occurred")
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }
}
```

### 5.5 Cấu hình Logging trong `application.properties`

```properties
# ===== Logging =====
# Root level (mặc định cho toàn bộ app)
logging.level.root=INFO

# Level cho package cụ thể
logging.level.student.management.api_app=DEBUG
logging.level.student.management.api_app.controller=DEBUG
logging.level.student.management.api_app.service=DEBUG

# Hibernate SQL logging (đã có sẵn)
logging.level.org.hibernate.SQL=DEBUG
# Hiển thị tham số bind trong SQL query
logging.level.org.hibernate.orm.jdbc.bind=TRACE

# Log ra file (tuỳ chọn)
logging.file.name=logs/app.log
logging.file.max-size=10MB
logging.file.max-history=30

# Custom log pattern (tuỳ chọn)
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### 5.6 Tùy chỉnh nâng cao với `logback-spring.xml`

Nếu cần cấu hình chi tiết hơn (nhiều appender, rolling policy, filter), tạo file:

```xml
<!-- src/main/resources/logback-spring.xml -->

<configuration>
    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File appender (rolling daily) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- Tạo file mới mỗi ngày, giữ tối đa 30 ngày -->
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- App package: DEBUG level -->
    <logger name="student.management.api_app" level="DEBUG"/>

    <!-- Hibernate SQL -->
    <logger name="org.hibernate.SQL" level="DEBUG"/>

    <!-- Root: INFO, gửi log ra cả console và file -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

> **Lưu ý**: Nếu có file `logback-spring.xml` thì Spring Boot sẽ ưu tiên dùng file này thay cho `application.properties` phần logging

### 5.7 So sánh Log4j2, Logback và SLF4J

| Tiêu chí            | Log4j2                              | Logback                         | SLF4J                    |
|----------------------|-------------------------------------|---------------------------------|--------------------------|
| Vai trò              | Implementation                      | Implementation (mặc định)      | Facade (interface)       |
| Hiệu năng           | Async logging nhanh nhất            | Tốt cho hầu hết use case       | Không xử lý, chỉ chuyển |
| Cấu hình             | XML / JSON / YAML / Properties      | XML (`logback.xml`)             | Không có cấu hình riêng |
| Mặc định Spring Boot | Không                               | ✅ Có                           | ✅ Có                    |
| Dùng khi             | Cần async logging, high throughput  | Hầu hết project Spring Boot    | Viết code logging        |

> * Trong project Spring Boot, **SLF4J + Logback** là đủ dùng cho phần lớn trường hợp
> * Chỉ cần đổi sang Log4j2 khi có yêu cầu đặc biệt về performance (very high throughput)
> * Nhờ SLF4J là facade, nếu muốn đổi từ Logback sang Log4j2 chỉ cần thay dependency, **không cần sửa code**

### 5.8 Chuyển sang Log4j2 (tham khảo)

Trường hợp muốn dùng **Log4j2** thay Logback:

#### Bước 1: Loại bỏ Logback, thêm Log4j2

```groovy
// build.gradle

configurations.all {
    exclude group: 'org.springframework.boot', module: 'spring-boot-starter-logging'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-log4j2'
}
```

#### Bước 2: Tạo file cấu hình `log4j2-spring.xml`

```xml
<!-- src/main/resources/log4j2-spring.xml -->

<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
        </Console>

        <RollingFile name="File" fileName="logs/app.log"
                     filePattern="logs/app-%d{yyyy-MM-dd}.log">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"/>
            <Policies>
                <TimeBasedTriggeringPolicy/>
            </Policies>
            <DefaultRolloverStrategy max="30"/>
        </RollingFile>
    </Appenders>

    <Loggers>
        <Logger name="student.management.api_app" level="DEBUG"/>
        <Logger name="org.hibernate.SQL" level="DEBUG"/>

        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="File"/>
        </Root>
    </Loggers>
</Configuration>
```

> **Lưu ý**: Code Java (`@Slf4j`, `log.info(...)`) **KHÔNG CẦN SỬA**, vì dùng SLF4J facade

---

## 6) Thực hành

### 6.1 Thêm Validation vào DTO

> * Thêm annotation validation cho `PersonCreateRequest`, `StudentCreateOnlyRequest`, `StudentCreateRequest`
> * Thêm `@Valid` vào `@RequestBody` ở `StudentController.create()`
> * Test bằng Postman với dữ liệu invalid → xem response mặc định của Spring

### 6.2 Tạo `GlobalExceptionHandler`

> * Tạo class `GlobalExceptionHandler` với `@RestControllerAdvice`
> * Bắt `MethodArgumentNotValidException` → trả về `AppResponse` có danh sách field errors
> * Bắt `ResponseStatusException` → trả về `AppResponse` phù hợp
> * Bắt `Exception` (fallback) → trả về 500 với message chung
> * Test lại bằng Postman → so sánh response trước và sau khi có handler

### 6.3 Tạo Custom Validation `@AdultAge`

> * Tạo package `validation`
> * Tạo annotation `@AdultAge` và class `AdultAgeValidator`
> * Áp dụng vào field `dob` của `PersonCreateRequest`
> * Test với ngày sinh khiến tuổi < 16

### 6.4 Thêm Logging

> * Thêm `@Slf4j` vào `StudentService` và `GlobalExceptionHandler`
> * Thêm `log.info`, `log.warn`, `log.error` ở các vị trí phù hợp
> * Thêm cấu hình logging trong `application.properties`
> * Chạy app, gọi API, quan sát log output trên console

### 6.5 Bài tập: Tạo annotation `@ValidMajorCode`

> * Yêu cầu: mã chuyên ngành phải là 2-4 ký tự in hoa, chỉ chứa chữ cái (ví dụ: "IT", "SE", "AI", "BA")
> * Gợi ý: regex `^[A-Z]{2,4}$`

```java
// Bước 1: Tạo @ValidMajorCode annotation
// Hãy hoàn thiện code

// Bước 2: Tạo ValidMajorCodeValidator
// Hãy hoàn thiện code

// Bước 3: Sử dụng trong MajorCreateRequest
// Hãy hoàn thiện code
```

### 6.6 Bài tập tổng hợp

> * Tạo annotation `@StudentCodeFormat` cho field `studentCode`
> * Tạo `ResourceNotFoundException` và handler tương ứng
> * Refactor tất cả `ResponseStatusException(NOT_FOUND)` trong service thành `ResourceNotFoundException`
> * Thêm cấu hình ghi log ra file `logs/app.log`
