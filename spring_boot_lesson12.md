# Spring Boot – Buổi 12: Spring Security Setup

## 1) Tổng quan về Spring Security

### 1.1 Spring Security là gì?

> **Spring Security** là framework bảo mật mạnh mẽ nhất trong hệ sinh thái Spring, cung cấp giải pháp toàn diện cho **Xác thực (Authentication)** và **Phân quyền (Authorization)** trong ứng dụng Java.

Ở Buổi 11, chúng ta đã tìm hiểu lý thuyết về các lỗ hổng bảo mật (OWASP Top 10). Bây giờ là lúc áp dụng vào code thực tế: cài đặt Spring Security để bảo vệ các API endpoint.

### 1.2 Hai khái niệm nền tảng

| Khái niệm | Tiếng Việt | Câu hỏi nó trả lời | Ví dụ thực tế |
|---|---|---|---|
| **Authentication** | Xác thực | "Bạn **là ai**?" | Đăng nhập bằng username/password, nhận JWT Token |
| **Authorization** | Phân quyền | "Bạn **được phép làm gì**?" | User thường chỉ xem, Admin được phép xoá |

```
                    ┌──────────────────────────────────┐
  HTTP Request ────►│        Spring Security           │
                    │                                  │
                    │  1. Authentication Filter         │
                    │     → Bạn là ai? (Token hợp lệ?) │
                    │                                  │
                    │  2. Authorization Filter           │
                    │     → Bạn có quyền truy cập?     │
                    │                                  │
                    └──────────┬───────────────────────┘
                               │ ✅ Pass
                               ▼
                    ┌──────────────────────────────────┐
                    │     Controller / Service          │
                    └──────────────────────────────────┘
```

### 1.3 Luồng hoạt động tổng quát (Security Filter Chain)

Khi một HTTP Request đến ứng dụng Spring Boot đã cài Spring Security, nó sẽ đi qua một chuỗi các **Filter** (Security Filter Chain) trước khi đến `Controller`:

```
Client ──► Filter 1 ──► Filter 2 ──► ... ──► Filter N ──► DispatcherServlet ──► Controller
           (CORS)       (Auth)         ...      (Authz)
```

Một số Filter quan trọng:
- **CorsFilter**: Xử lý Cross-Origin Resource Sharing
- **UsernamePasswordAuthenticationFilter**: Xử lý đăng nhập bằng username/password
- **BearerTokenAuthenticationFilter** (hoặc custom JWT filter): Xử lý xác thực bằng JWT Token
- **AuthorizationFilter**: Kiểm tra quyền truy cập dựa trên role/authority

> **Lưu ý:** Khi thêm dependency `spring-boot-starter-security`, **mặc định TẤT CẢ endpoint đều bị khoá** (trả về 401 Unauthorized). Chúng ta cần cấu hình để mở cho những endpoint public.

---

## 2) Cài đặt Spring Security

### 2.1 Thêm dependency

Thêm vào `build.gradle`:

```groovy
dependencies {
    // ... các dependency hiện có ...
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
}
```

| Dependency | Vai trò |
|---|---|
| `spring-boot-starter-security` | Module chính của Spring Security |
| `jjwt-api` | Thư viện tạo & parse JSON Web Token |
| `jjwt-impl` | Implementation của JJWT |
| `jjwt-jackson` | Hỗ trợ serialize/deserialize JWT bằng Jackson |

### 2.2 Thêm cấu hình JWT vào `application.yml`

```yaml
# application.yml
jwt:
  secret-key: "my-super-secret-key-for-jwt-must-be-at-least-256-bits-long-1234567890"
  expiration-ms: 86400000  # 24 giờ = 86400000 ms
```

> ⚠️ **KHÔNG BAO GIỜ** hardcode secret key trên production. Hãy dùng **biến môi trường (Environment Variable)** hoặc **Vault** để quản lý.

---

## 3) Tạo Entity User & Enum Role

### 3.1 Enum Role

```java
package student.management.api_app.constant;

public enum Role {
    USER,
    ADMIN
}
```

> Trong thực tế lớn hơn, Role có thể là một Entity riêng với bảng `roles` trên DB (many-to-many với User). Nhưng với dự án học, dùng **Enum** là đủ và gọn gàng.

### 3.2 Entity User

```java
package student.management.api_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import student.management.api_app.constant.Role;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users", schema = "app")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(unique = true, nullable = false, length = 50)
    String username;

    @Column(nullable = false)
    String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Role role;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    // ===== Implement UserDetails =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Trả về danh sách quyền. Prefix "ROLE_" là convention của Spring Security
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return isActive; }
}
```

**Giải thích quan trọng:**
- `UserDetails` là interface của Spring Security, đại diện cho thông tin người dùng đã xác thực.
- `getAuthorities()` trả về **danh sách quyền** (roles/permissions). Spring Security dùng prefix `"ROLE_"` để phân biệt **Role** với **Authority** thông thường.
- Các method `isAccountNonExpired()`, `isAccountNonLocked()`, v.v. cho phép kiểm soát trạng thái tài khoản (khoá, hết hạn...).

### 3.3 Tạo bảng trên Database

```sql
-- SQL tạo bảng users
CREATE TABLE app.users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

### 3.4 UserRepository

```java
package student.management.api_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import student.management.api_app.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

---

## 4) Tạo JWT Utility

### 4.1 JwtUtil – Tạo và xác thực Token

```java
package student.management.api_app.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // ===== Tạo Token =====
    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("role", userDetails.getAuthorities().iterator().next().getAuthority())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ===== Trích xuất thông tin từ Token =====
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ===== Kiểm tra Token hợp lệ =====
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
```

**Các thành phần của JWT Token:**

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzEyMzQ1Njc4LCJleHAiOjE3MTI0MzIwNzh9.abc123signature
└──── Header ────┘ └───────────────────── Payload ──────────────────────┘ └── Signature ──┘
```

| Phần | Nội dung | Ví dụ |
|---|---|---|
| **Header** | Thuật toán ký | `{"alg": "HS256"}` |
| **Payload** | Dữ liệu (claims) | `{"sub": "admin", "role": "ROLE_ADMIN", "exp": 1712432078}` |
| **Signature** | Chữ ký bảo mật | HMAC-SHA256(Header + Payload, SecretKey) |

---

## 5) Cấu hình Spring Security (SecurityConfig)

### 5.1 UserDetailsService – Nạp thông tin User từ DB

```java
package student.management.api_app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import student.management.api_app.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy user: " + username
                ));
    }
}
```

> `UserDetailsService` là interface mà Spring Security gọi khi cần tìm thông tin User từ hệ thống (DB, LDAP, v.v.).

### 5.2 JwtAuthenticationFilter – Xác thực JWT mỗi request

> **Tại sao dùng `OncePerRequestFilter`?**
>
> `OncePerRequestFilter` đảm bảo filter chỉ chạy **đúng 1 lần** cho mỗi HTTP request. Nếu dùng `Filter` thông thường, khi request bị **forward** (ví dụ: `DispatcherServlet` forward tới error page), filter sẽ chạy lại lần 2 → gây xác thực trùng lặp hoặc lỗi.

```java
package student.management.api_app.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import student.management.api_app.util.JwtUtil;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Lấy header Authorization
        final String authHeader = request.getHeader("Authorization");

        // 2. Nếu không có header hoặc không bắt đầu bằng "Bearer " → bỏ qua
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Trích xuất token (bỏ 7 ký tự đầu = "Bearer ")
        //    "Bearer eyJhbGci..."
        //     0123456^
        //            substring(7) → "eyJhbGci..."
        final String jwt = authHeader.substring(7);
        final String username;

        try {
            username = jwtUtil.extractUsername(jwt);
        } catch (Exception e) {
            // Token không hợp lệ hoặc hết hạn → bỏ qua, để request tiếp tục như anonymous
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Nếu username hợp lệ và chưa được xác thực
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 5. Kiểm tra token có hợp lệ không
            if (jwtUtil.isTokenValid(jwt, userDetails)) {
                // Tạo Authentication token:
                //   - principal  = userDetails (thông tin user)
                //   - credentials = null (không cần giữ password trong bộ nhớ sau khi đã xác thực xong)
                //   - authorities = danh sách quyền của user
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,  // credentials = null vì token đã xác thực, không cần password nữa
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 6. Đặt vào SecurityContext → các filter/controller sau đó biết user đã xác thực
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
```

**Giải thích luồng:**

```
                     Request có header "Authorization: Bearer eyJ..."
                                       │
                                       ▼
                        ┌─────────────────────────┐
                        │   JwtAuthenticationFilter │
                        │                           │
                        │  1. Trích xuất JWT        │
                        │  2. Parse username         │
                        │  3. Load UserDetails từ DB │
                        │  4. Xác thực token         │
                        │  5. Set SecurityContext    │
                        └─────────┬─────────────────┘
                                  │ ✅ Authenticated
                                  ▼
                         Controller xử lý request
```

### 5.3 SecurityConfig – Cấu hình chính

```java
package student.management.api_app.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Bật hỗ trợ @PreAuthorize, @Secured, @RolesAllowed
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Tắt CSRF (vì dùng JWT, stateless, không cần CSRF token)
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Cấu hình quyền truy cập cho các endpoint
            //    LƯU Ý: Thứ tự QUAN TRỌNG – Spring kiểm tra từ trên xuống, rule đầu tiên khớp sẽ được áp dụng
            .authorizeHttpRequests(auth -> auth
                // Cho phép truy cập không cần đăng nhập
                .requestMatchers("/api/v1/auth/**").permitAll()  // ← phải đặt TRƯỚC rule POST bên dưới
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Các API GET cho phép tất cả (public read)
                .requestMatchers(HttpMethod.GET, "/api/v1/students/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/majors/**").permitAll()

                // Các API tạo/sửa/xoá yêu cầu đăng nhập
                .requestMatchers(HttpMethod.POST, "/api/v1/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").authenticated()

                // Tất cả request còn lại yêu cầu đăng nhập
                .anyRequest().authenticated()
            )

            // 3. Stateless session (không lưu session trên server)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 4. Custom xử lý lỗi 401/403
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthenticationEntryPoint)  // 401
                .accessDeniedHandler(customAccessDeniedHandler)            // 403
            )

            // 5. Đăng ký AuthenticationProvider
            .authenticationProvider(authenticationProvider())

            // 6. Thêm JWT filter TRƯỚC UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Giải thích từng phần cấu hình:**

| Cấu hình | Giải thích |
|---|---|
| `csrf().disable()` | Tắt CSRF vì API dùng JWT (stateless), không dùng cookie session |
| `authorizeHttpRequests()` | Quy định endpoint nào public, endpoint nào cần đăng nhập. **Thứ tự rule rất quan trọng** – Spring kiểm tra từ trên xuống, rule đầu tiên khớp sẽ thắng |
| `SessionCreationPolicy.STATELESS` | Không tạo HTTP Session – mỗi request tự xác thực qua JWT |
| `addFilterBefore(...)` | Chèn JWT filter vào trước filter mặc định của Spring Security |
| `BCryptPasswordEncoder` | Mã hoá mật khẩu bằng BCrypt (one-way hash + random salt). Cùng 1 password hash 2 lần sẽ ra 2 chuỗi khác nhau, nhưng BCrypt vẫn verify đúng |
| `@EnableMethodSecurity` | Kích hoạt `@PreAuthorize` để phân quyền ở cấp method |

---

## 6) Tạo API Đăng nhập (Authentication)

### 6.1 DTO Request / Response

```java
// === AuthRequest.java ===
package student.management.api_app.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank(message = "Username không được để trống")
    String username,

    @NotBlank(message = "Password không được để trống")
    String password
) {}
```

```java
// === AuthResponse.java ===
package student.management.api_app.dto.auth;

public record AuthResponse(
    String token,
    String username,
    String role
) {}
```

### 6.2 AuthController

```java
package student.management.api_app.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import student.management.api_app.dto.auth.AuthRequest;
import student.management.api_app.dto.auth.AuthResponse;
import student.management.api_app.util.JwtUtil;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {

        log.info("Login attempt for user: {}", request.username());

        // 1. Xác thực username & password
        //    Nếu sai password → AuthenticationManager ném BadCredentialsException
        //    Nếu user không tồn tại → ném UsernameNotFoundException
        //    Cả hai đều được bắt bởi GlobalExceptionHandler (xem section 8.4)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // 2. Lấy UserDetails sau khi xác thực thành công
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Tạo JWT Token
        String token = jwtUtil.generateToken(userDetails);

        // 4. Trả về token + thông tin user
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        log.info("Login successful for user: {}, role: {}", userDetails.getUsername(), role);
        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername(), role));
    }
}
```

**Luồng đăng nhập hoàn chỉnh:**

```
Client                          Server
  │                               │
  │  POST /api/v1/auth/login      │
  │  { "username": "admin",       │
  │    "password": "123456" }     │
  │ ─────────────────────────────►│
  │                               │  1. AuthenticationManager xác thực
  │                               │  2. So sánh password (BCrypt)
  │                               │  3. Tạo JWT Token
  │  ◄────────────────────────────│
  │  { "token": "eyJ...",         │
  │    "username": "admin",       │
  │    "role": "ROLE_ADMIN" }     │
  │                               │
  │  GET /api/v1/students         │
  │  Header: Authorization:       │
  │    Bearer eyJ...              │
  │ ─────────────────────────────►│
  │                               │  4. JwtAuthFilter xác thực token
  │                               │  5. Controller xử lý bình thường
  │  ◄────────────────────────────│
  │  { "data": [...] }            │
```

---

## 7) Phân quyền với @PreAuthorize

### 7.1 @PreAuthorize là gì?

`@PreAuthorize` là annotation dùng để kiểm tra quyền **trước khi method được thực thi**. Nó hoạt động nhờ `@EnableMethodSecurity` đã khai báo trong `SecurityConfig`.

### 7.2 Cú pháp cơ bản

```java
// Chỉ cho phép user có ROLE_ADMIN
@PreAuthorize("hasRole('ADMIN')")

// Cho phép ADMIN hoặc USER
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")

// Cho phép tất cả user đã đăng nhập
@PreAuthorize("isAuthenticated()")

// Cho phép dựa trên authority cụ thể (không tự thêm prefix)
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
```

> **Phân biệt `hasRole` và `hasAuthority`:**
>
> | Biểu thức | Spring Security xử lý | Kết quả kiểm tra |
> |---|---|---|
> | `hasRole('ADMIN')` | Tự thêm prefix `"ROLE_"` | Kiểm tra user có authority `"ROLE_ADMIN"` |
> | `hasAuthority('ROLE_ADMIN')` | Không thêm gì | Kiểm tra user có authority `"ROLE_ADMIN"` |
> | `hasAuthority('ADMIN')` | Không thêm gì | Kiểm tra user có authority `"ADMIN"` (**sẽ FAIL** vì entity trả về `"ROLE_ADMIN"`) |
>
> **Khuyến nghị:** Dùng `hasRole('ADMIN')` cho đơn giản. Chỉ dùng `hasAuthority()` khi bạn có các permission không theo convention `ROLE_*` (ví dụ: `hasAuthority('READ_REPORTS')`).

### 7.3 Áp dụng @PreAuthorize ở Controller

```java
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ✅ PUBLIC – Ai cũng xem được danh sách (đã cấu hình ở SecurityConfig)
    @GetMapping
    public ResponseEntity<?> getAll(/* ... */) {
        // ...
    }

    // 🔒 Chỉ user đã đăng nhập mới được tạo
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody StudentCreateRequest request) {
        // ...
    }

    // 🔒 Chỉ ADMIN mới được xoá
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        // ...
    }

    // 🔒 ADMIN hoặc USER đều có thể cập nhật
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable UUID id,
            @Valid @RequestBody StudentUpdateRequest request
    ) {
        // ...
    }
}
```

### 7.4 @PreAuthorize với SpEL nâng cao

Spring Expression Language (SpEL) cho phép viết điều kiện phức tạp hơn:

```java
// Kiểm tra user hiện tại trùng với tham số
@PreAuthorize("#username == authentication.principal.username")
@GetMapping("/profile/{username}")
public ResponseEntity<?> getProfile(@PathVariable String username) {
    // Chỉ user đang đăng nhập mới xem được profile của chính mình
}

// Kết hợp nhiều điều kiện (OR)
@PreAuthorize("hasRole('ADMIN') or #username == authentication.principal.username")
@PutMapping("/profile/{username}")
public ResponseEntity<?> updateProfile(
        @PathVariable String username,
        @RequestBody UpdateProfileRequest request
) {
    // Admin được sửa profile bất kỳ
    // User thường chỉ sửa profile của chính mình
}
```

### 7.5 So sánh các cách phân quyền

| Cách | Ưu điểm | Nhược điểm | Khi nào dùng? |
|---|---|---|---|
| `SecurityConfig` (`authorizeHttpRequests`) | Tập trung, nhìn tổng quan nhanh | Khó xử lý logic phân quyền phức tạp | Phân quyền theo URL pattern đơn giản |
| `@PreAuthorize` trên method | Linh hoạt, gần code xử lý, hỗ trợ SpEL | Phân tán ở nhiều nơi | Logic phân quyền phức tạp, cần tham số |
| Kết hợp cả hai | Tốt nhất | Cần đồng bộ cả hai nơi | **Khuyến nghị cho dự án thực tế** |

---

## 8) Xử lý lỗi Security (Exception Handling)

Khi user chưa đăng nhập hoặc không đủ quyền, Spring Security sẽ trả về lỗi. Chúng ta nên custom response để nhất quán với format API.

### 8.1 Custom AuthenticationEntryPoint (401 Unauthorized)

```java
package student.management.api_app.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 401,
                "error", "Unauthorized",
                "message", "Bạn cần đăng nhập để truy cập tài nguyên này",
                "path", request.getRequestURI()
        );

        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}
```

### 8.2 Custom AccessDeniedHandler (403 Forbidden)

```java
package student.management.api_app.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpStatus.FORBIDDEN.value());

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "message", "Bạn không có quyền truy cập tài nguyên này",
                "path", request.getRequestURI()
        );

        new ObjectMapper().writeValue(response.getOutputStream(), body);
    }
}
```

### 8.3 Đăng ký vào SecurityConfig

Ở phần **5.3 SecurityConfig** bên trên, chúng ta đã tích hợp sẵn 2 handler này qua đoạn cấu hình:

```java
// Trích từ SecurityConfig.securityFilterChain() – xem lại section 5.3 để thấy code đầy đủ
.exceptionHandling(ex -> ex
    .authenticationEntryPoint(customAuthenticationEntryPoint)  // 401
    .accessDeniedHandler(customAccessDeniedHandler)            // 403
)
```

> Nếu **không** cấu hình `.exceptionHandling(...)`, Spring Security sẽ trả về trang HTML mặc định (hoặc JSON response thiếu chi tiết), không nhất quán với format API của dự án.

**Kết quả:**

```json
// 401 – Khi gọi API mà không có token
{
    "timestamp": "2026-04-06T00:00:00Z",
    "status": 401,
    "error": "Unauthorized",
    "message": "Bạn cần đăng nhập để truy cập tài nguyên này",
    "path": "/api/v1/students"
}

// 403 – Khi user không đủ quyền (ví dụ USER gọi API chỉ ADMIN mới được)
{
    "timestamp": "2026-04-06T00:00:00Z",
    "status": 403,
    "error": "Forbidden",
    "message": "Bạn không có quyền truy cập tài nguyên này",
    "path": "/api/v1/students/123"
}
```

### 8.4 Xử lý lỗi đăng nhập sai (BadCredentialsException)

Khi user đăng nhập với sai password, `AuthenticationManager.authenticate()` sẽ ném `BadCredentialsException`. Nếu không bắt exception này, Spring sẽ trả về **500 Internal Server Error** – gây nhầm lẫn cho sinh viên.

Thêm vào `GlobalExceptionHandler` (hoặc tạo mới nếu chưa có):

```java
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

// Bắt lỗi đăng nhập sai username hoặc password
@ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
public ResponseEntity<AppResponse<Void>> handleBadCredentials(
        Exception ex,
        HttpServletRequest request) {

    log.warn("Login failed at [{}]: {}", request.getRequestURI(), ex.getMessage());

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            AppResponse.<Void>builder()
                    .success(false)
                    .error(AppResponse.AppError.builder()
                            .code("LOGIN_FAILED")
                            .message("Sai username hoặc password")
                            .path(request.getRequestURI())
                            .build())
                    .build()
    );
}
```

**Kết quả khi login sai:**

```json
// POST /api/v1/auth/login với password sai
{
    "success": false,
    "data": null,
    "error": {
        "code": "LOGIN_FAILED",
        "message": "Sai username hoặc password",
        "path": "/api/v1/auth/login"
    }
}
```

> ⚠️ **Bảo mật:** Luôn trả về message chung `"Sai username hoặc password"` thay vì `"Username không tồn tại"` hay `"Password sai"`. Điều này ngăn hacker **enumerate** (dò) xem username nào tồn tại trong hệ thống.

---

## 9) JPA Auditing – Tự động quản lý người tạo / người sửa

### 9.1 JPA Auditing là gì?

Trong các ứng dụng thực tế, mỗi bản ghi thường cần lưu lại:
- **Ai tạo** bản ghi này? (`createdBy`)
- **Ai sửa** lần cuối? (`updatedBy`)
- **Khi nào tạo?** (`createdAt`)
- **Khi nào sửa?** (`updatedAt`)

**JPA Auditing** là cơ chế của Spring Data JPA tự động điền các giá trị này mà **không cần viết code thủ công** trong Service.

### 9.2 Bật JPA Auditing

**Bước 1:** Thêm `@EnableJpaAuditing` vào class Application hoặc Config:

```java
package student.management.api_app.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("SYSTEM");
            }

            return Optional.of(authentication.getName());
        };
    }
}
```

**Giải thích:**
- `@EnableJpaAuditing` kích hoạt tính năng Auditing.
- `AuditorAware<String>` là interface cho phép Spring biết **user hiện tại là ai**. Ở đây ta lấy từ `SecurityContextHolder` (chính là user đã xác thực qua JWT).
- Nếu không có user đăng nhập (ví dụ job chạy nền), trả về `"SYSTEM"`.

### 9.3 Tạo Base Entity với Auditing fields

```java
package student.management.api_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 50)
    String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 50)
    String updatedBy;
}
```

**Giải thích các Annotation:**

| Annotation | Chức năng |
|---|---|
| `@MappedSuperclass` | Các field của class này được kế thừa vào bảng của Entity con (không tạo bảng riêng) |
| `@EntityListeners(AuditingEntityListener.class)` | Đăng ký listener để Spring Data tự động điền các field auditing |
| `@CreatedDate` | Tự động điền thời gian tạo (chỉ 1 lần khi INSERT) |
| `@LastModifiedDate` | Tự động cập nhật thời gian sửa (mỗi lần UPDATE) |
| `@CreatedBy` | Tự động điền username người tạo |
| `@LastModifiedBy` | Tự động cập nhật username người sửa lần cuối |

### 9.4 Áp dụng vào Entity hiện có

Thay vì khai báo `createdAt`, `updatedAt` riêng lẻ trong từng Entity, giờ chỉ cần **kế thừa** `BaseEntity`:

```java
// === TRƯỚC KHI DÙNG AUDITING ===
@Entity
@Table(name = "people", schema = "app")
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    String fullName;
    // ... các field khác ...

    @Column(name = "created_at", insertable = false, updatable = false)
    Instant createdAt;     // ← Khai báo thủ công, không biết ai tạo

    @Column(name = "updated_at", insertable = false, updatable = false)
    Instant updatedAt;     // ← Khai báo thủ công, không biết ai sửa
}

// === SAU KHI DÙNG AUDITING ===
@Entity
@Table(name = "people", schema = "app")
public class Person extends BaseEntity {   // ← Kế thừa BaseEntity
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    String fullName;
    // ... các field khác ...

    // Không cần khai báo createdAt, updatedAt nữa!
    // Thêm tự động: createdBy, updatedBy
}
```

### 9.5 Thêm cột vào Database

Nếu DB hiện tại chưa có cột `created_by` và `updated_by`, cần chạy migration:

```sql
-- Thêm cột auditing vào bảng people
ALTER TABLE app.people
    ADD COLUMN created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN updated_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM';

-- Thêm tương tự cho các bảng khác (students, majors, ...)
ALTER TABLE app.students
    ADD COLUMN created_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN updated_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM';
```

> ⚠️ **Lưu ý quan trọng:** Nếu bảng hiện tại đang dùng **DB trigger** để tự động điền `created_at` / `updated_at` (ví dụ: `DEFAULT NOW()` hoặc trigger `ON UPDATE`), bạn cần **xoá trigger đó** sau khi chuyển sang JPA Auditing. Nếu không, sẽ xảy ra xung đột: JPA set giá trị một đường, trigger lại ghi đè theo đường khác.
>
> Đồng thời, cần **xoá** thuộc tính `insertable = false, updatable = false` trên các cột `created_at`, `updated_at` trong Entity cũ (vì giờ JPA Auditing chịu trách nhiệm ghi giá trị, không còn phụ thuộc DB).

### 9.6 Kết quả khi gọi API

Sau khi tích hợp xong, khi tạo hoặc cập nhật dữ liệu qua API:

```json
// POST /api/v1/students (đăng nhập với user "admin")
// Response sẽ bao gồm:
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "studentCode": "STU001",
    "fullName": "Nguyen Van A",
    "createdAt": "2026-04-06T05:00:00Z",
    "updatedAt": "2026-04-06T05:00:00Z",
    "createdBy": "admin",      // ← JPA Auditing tự động điền
    "updatedBy": "admin"       // ← JPA Auditing tự động điền
}
```

```json
// PUT /api/v1/students/550e... (đăng nhập với user "teacher01")
{
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "studentCode": "STU001",
    "fullName": "Nguyen Van A (Updated)",
    "createdAt": "2026-04-06T05:00:00Z",    // ← Không đổi
    "updatedAt": "2026-04-06T06:30:00Z",    // ← Cập nhật
    "createdBy": "admin",                    // ← Không đổi
    "updatedBy": "teacher01"                 // ← Cập nhật thành user sửa
}
```

---

## 10) Lỗi hay gặp khi làm Spring Security

### ❌ Lỗi 1: Quên `@EnableMethodSecurity` → `@PreAuthorize` không hoạt động

```java
// ❌ SAI – @PreAuthorize sẽ bị bỏ qua IM LẶNG (không báo lỗi, không chặn gì cả!)
@Configuration
@EnableWebSecurity
// Thiếu: @EnableMethodSecurity
public class SecurityConfig { ... }

// ✅ ĐÚNG
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // Bật hỗ trợ @PreAuthorize
public class SecurityConfig { ... }
```

> Đây là lỗi **nguy hiểm nhất** vì không có báo lỗi nào – app chạy bình thường, nhưng `@PreAuthorize` hoàn toàn vô hiệu.

### ❌ Lỗi 2: Secret key quá ngắn → `WeakKeyException`

```yaml
# ❌ SAI – JJWT yêu cầu key ≥ 256 bits (32 bytes) cho HS256
jwt:
  secret-key: "my-secret"

# ✅ ĐÚNG – Key đủ dài (≥ 32 ký tự)
jwt:
  secret-key: "my-super-secret-key-for-jwt-must-be-at-least-256-bits-long-1234567890"
```

### ❌ Lỗi 3: Thứ tự rule trong `authorizeHttpRequests` sai → Login bị chặn

```java
// ❌ SAI – Rule POST bắt tất cả /api/v1/** đặt TRƯỚC rule permitAll cho /auth/**
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.POST, "/api/v1/**").authenticated()  // Khớp trước!
    .requestMatchers("/api/v1/auth/**").permitAll()                  // Không bao giờ đến được
)
// Kết quả: POST /api/v1/auth/login cũng bị yêu cầu token → không login được!

// ✅ ĐÚNG – Rule cụ thể hơn (đặc biệt là permitAll) phải đặt TRƯỚC
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/auth/**").permitAll()                  // Khớp trước!
    .requestMatchers(HttpMethod.POST, "/api/v1/**").authenticated()  // Chỉ áp dụng cho còn lại
)
```

### ❌ Lỗi 4: Không bắt `BadCredentialsException` → Login sai trả về 500

```
POST /api/v1/auth/login { "username": "admin", "password": "sai_mat_khau" }

❌ Không có handler: 500 Internal Server Error (sinh viên nghĩ code bị lỗi)
✅ Có handler:        401 Unauthorized + message "Sai username hoặc password"
```

> Xem lại section **8.4** để thêm handler này vào `GlobalExceptionHandler`.

### ❌ Lỗi 5: Gọi API với token hết hạn → Nhận 500 thay vì 401

```java
// ❌ SAI – JwtAuthenticationFilter không bắt ExpiredJwtException
// Token hết hạn → jwtUtil.extractUsername() ném Exception → 500 Internal Server Error

// ✅ ĐÚNG – Bọc lại try-catch trong filter
try {
    username = jwtUtil.extractUsername(jwt);
} catch (Exception e) {
    // Token không hợp lệ hoặc hết hạn → bỏ qua, request tiếp tục như anonymous
    filterChain.doFilter(request, response);
    return;
}
```

### ❌ Lỗi 6: Nhầm `hasRole('ROLE_ADMIN')` thay vì `hasRole('ADMIN')`

```java
// ❌ SAI – hasRole tự thêm prefix "ROLE_" → thành kiểm tra "ROLE_ROLE_ADMIN"
@PreAuthorize("hasRole('ROLE_ADMIN')")

// ✅ ĐÚNG – Chỉ truyền tên role (không có prefix)
@PreAuthorize("hasRole('ADMIN')")
```

### ❌ Lỗi 7: Entity `User` quên `@Builder.Default` cho `isActive`

```java
// ❌ SAI – Khi dùng User.builder().build(), isActive = null thay vì true
@Column(name = "is_active", nullable = false)
Boolean isActive = true;

// ✅ ĐÚNG – @Builder.Default cho Lombok biết giữ giá trị mặc định khi dùng Builder
@Column(name = "is_active", nullable = false)
@Builder.Default
Boolean isActive = true;
```

### ❌ Lỗi 8: Chuyển sang JPA Auditing nhưng quên xóa DB trigger cũ

```java
// ❌ SAI – Entity cũ có insertable=false, updatable=false (phụ thuộc DB trigger)
@Column(name = "created_at", insertable = false, updatable = false)
Instant createdAt;

// ✅ ĐÚNG – Khi dùng JPA Auditing, JPA chịu trách nhiệm ghi giá trị → bỏ insertable/updatable
// Và kế thừa từ BaseEntity (không khai báo lại)
public class Person extends BaseEntity {
    // KHÔNG cần khai báo createdAt, updatedAt nữa
}
```

> Đồng thời phải xóa DB trigger cũ (nếu có). Xem lại section **9.5** để biết chi tiết.

---

## 11) Tổng kết Buổi 12

### 11.1 Những gì đã học

| # | Nội dung | Ghi chú |
|---|---|---|
| 1 | Spring Security tổng quan | Authentication vs Authorization, Security Filter Chain |
| 2 | Cài đặt & cấu hình | Dependency, `SecurityConfig`, `SecurityFilterChain` |
| 3 | JWT Authentication | `JwtUtil`, `JwtAuthenticationFilter`, đăng nhập API |
| 4 | `@PreAuthorize` | Phân quyền ở cấp method, SpEL expressions |
| 5 | Xử lý lỗi Security | Custom 401, 403, `BadCredentialsException` |
| 6 | JPA Auditing | `@CreatedBy`, `@LastModifiedBy`, `BaseEntity`, `AuditorAware` |
| 7 | Lỗi hay gặp | 8 lỗi phổ biến và cách fix |

### 11.2 Checklist công việc

- [ ] Thêm dependency `spring-boot-starter-security` và `jjwt` vào `build.gradle`.
- [ ] Tạo Entity `User` implement `UserDetails`, tạo bảng `users` trên DB.
- [ ] Tạo `JwtUtil` để generate và validate JWT Token.
- [ ] Tạo `JwtAuthenticationFilter` để xác thực token mỗi request.
- [ ] Cấu hình `SecurityConfig` với `SecurityFilterChain`.
- [ ] Tạo API `/api/v1/auth/login` để đăng nhập.
- [ ] Bổ sung `@PreAuthorize` cho các API cần phân quyền.
- [ ] Cấu hình JPA Auditing: `BaseEntity`, `JpaAuditingConfig`.
- [ ] Test bằng Postman/Swagger: đăng nhập → lấy token → gọi API với `Authorization: Bearer <token>`.

---

## 12) Bài tập

### Bài tập 1: Hoàn thiện hệ thống Authentication (⭐ Bắt buộc)

**Yêu cầu:**
1. Tạo đầy đủ các class theo hướng dẫn ở trên: `User`, `UserRepository`, `JwtUtil`, `JwtAuthenticationFilter`, `SecurityConfig`, `AuthController`.
2. Tạo bảng `users` trên database và INSERT sẵn 2 bản ghi:
   - User `admin` với password đã hash bằng BCrypt, role `ADMIN`.
   - User `teacher01` với password đã hash bằng BCrypt, role `USER`.

> 💡 **Tip:** Dùng trang [https://bcrypt-generator.com](https://bcrypt-generator.com) hoặc viết một class `main` đơn giản để hash password:
> ```java
> public class PasswordGenerator {
>     public static void main(String[] args) {
>         BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
>         System.out.println(encoder.encode("123456"));
>     }
> }
> ```

3. Test bằng Postman:
   - `POST /api/v1/auth/login` với `{"username": "admin", "password": "123456"}` → nhận được JWT Token.
   - Gọi `GET /api/v1/students` (không cần token) → thành công.
   - Gọi `POST /api/v1/students` (không có token) → nhận 401 Unauthorized.
   - Gọi `POST /api/v1/students` (có token) → thành công.
   - Gọi `POST /api/v1/auth/login` với password sai → nhận 401 + message `"Sai username hoặc password"`.

---

### Bài tập 2: Phân quyền với @PreAuthorize (⭐ Bắt buộc)

**Yêu cầu:**
1. Đặt `@PreAuthorize` cho các Controller hiện có theo quy tắc:

| Endpoint | GET | POST | PUT | DELETE |
|---|---|---|---|---|
| `/api/v1/students/**` | `permitAll` | `isAuthenticated` | `isAuthenticated` | `hasRole('ADMIN')` |
| `/api/v1/majors/**` | `permitAll` | `hasRole('ADMIN')` | `hasRole('ADMIN')` | `hasRole('ADMIN')` |
| `/api/v1/persons/**` | `isAuthenticated` | `isAuthenticated` | `isAuthenticated` | `hasRole('ADMIN')` |

2. Test bằng Postman:
   - Đăng nhập bằng `teacher01` (role USER) → thử `DELETE /api/v1/students/{id}` → phải nhận 403 Forbidden.
   - Đăng nhập bằng `admin` (role ADMIN) → thử `DELETE /api/v1/students/{id}` → thành công.

---

### Bài tập 3: Tích hợp JPA Auditing (⭐ Bắt buộc)

**Yêu cầu:**
1. Tạo class `BaseEntity` với `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`.
2. Tạo class `JpaAuditingConfig` với `@EnableJpaAuditing` và `AuditorAware`.
3. Cho Entity `Person` kế thừa `BaseEntity`, xoá các field `createdAt`, `updatedAt` cũ.
4. Thêm cột `created_by`, `updated_by` vào bảng `people` trên database.
5. Test:
   - Đăng nhập bằng `admin` → Tạo 1 Person mới → Kiểm tra response có `createdBy: "admin"`.
   - Đăng nhập bằng `teacher01` → Cập nhật Person trên → Kiểm tra `updatedBy: "teacher01"` nhưng `createdBy` vẫn là `"admin"`.

---

### Bài tập 4: API Đăng ký tài khoản (⭐⭐ Nâng cao)

**Yêu cầu:**
1. Tạo API `POST /api/v1/auth/register` cho phép đăng ký tài khoản mới (public, không cần đăng nhập).
2. DTO `RegisterRequest` gồm: `username`, `password`, `confirmPassword`.
3. Validation:
   - `username` phải unique (kiểm tra trước khi tạo, trả lỗi 409 Conflict nếu trùng).
   - `password` phải ≥ 8 ký tự.
   - `confirmPassword` phải trùng với `password`.
4. Tài khoản mới tạo mặc định có role = `USER`.
5. Trả về response dạng `AuthResponse` (tự động login luôn sau khi đăng ký).

---

### Bài tập 5: Mở rộng BaseEntity cho tất cả Entity (⭐⭐ Nâng cao)

**Yêu cầu:**
1. Cho tất cả các Entity trong dự án (`Student`, `Major`, `Course`, `Enrollment`) kế thừa `BaseEntity`.
2. Thêm cột `created_by`, `updated_by` vào **tất cả** các bảng tương ứng trên database.
3. Đảm bảo ứng dụng chạy bình thường: tất cả API CRUD vẫn hoạt động, response trả về đầy đủ các field auditing.

**Chuẩn bị cho Buổi 13:**
Ở buổi tiếp theo, chúng ta sẽ tiếp tục với **JWT Refresh Token**, **Logout** (vô hiệu hoá token), và tìm hiểu sâu hơn về **CORS Configuration** cho phép Frontend (React/Angular) gọi API an toàn.
