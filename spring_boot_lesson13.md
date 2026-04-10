# Spring Boot – Buổi 13: JWT nâng cao, Refresh Token & Logout

## 1) Ôn tập nhanh: Authentication và Authorization

Ở Buổi 12, chúng ta đã cài đặt Spring Security với JWT. Trước khi mở rộng hệ thống, hãy nhắc lại hai khái niệm nền tảng:

| Khái niệm | Tiếng Việt | Câu hỏi | Ví dụ đã làm ở Buổi 12 |
|---|---|---|---|
| **Authentication** | Xác thực | "Bạn **là ai**?" | `POST /auth/login` → nhận JWT Token |
| **Authorization** | Phân quyền | "Bạn **được phép làm gì**?" | `@PreAuthorize("hasRole('ADMIN')")` → chỉ ADMIN xoá được |

> 💡 Hai khái niệm này **luôn đi cùng nhau**: Authentication xác định danh tính → Authorization kiểm tra quyền dựa trên danh tính đó. Thiếu bất kỳ cái nào, hệ thống đều không an toàn.

Ở buổi hôm nay, chúng ta sẽ đi sâu vào **lý thuyết nền** (tại sao chọn BCrypt thay vì MD5? JWT cấu tạo thế nào?) và mở rộng hệ thống với **Refresh Token** + **Logout**.

---

## 2) MD5 và lý do không nên dùng cho mật khẩu

Ở Buổi 12, chúng ta đã dùng `BCryptPasswordEncoder` để hash mật khẩu mà chưa giải thích **tại sao chọn BCrypt**. Để hiểu rõ, hãy bắt đầu từ MD5 – thuật toán hash mà nhiều hệ thống cũ vẫn dùng sai mục đích.

### 2.1 MD5 là gì?

**MD5 (Message Digest 5)** là thuật toán hash tạo ra chuỗi 128-bit (32 ký tự hex) từ bất kỳ dữ liệu đầu vào nào.

```
Input: "123456"    → MD5: "e10adc3949ba59abbe56e057f20f883e"
Input: "password"  → MD5: "5f4dcc3b5aa765d61d8327deb882cf99"
Input: "123457"    → MD5: "f1887d3f9e6ee7a32fe5e76f4ab80b4f"  (khác hoàn toàn chỉ vì thay đổi 1 ký tự)
```

### 2.2 Đặc điểm của MD5

| Đặc điểm | Mô tả |
|---|---|
| **One-way** | Không thể đảo ngược (từ hash → ra plaintext) |
| **Deterministic** | Cùng input luôn ra cùng output |
| **Fixed length** | Output luôn là 32 ký tự hex (128-bit) |
| **Nhanh** | Hash rất nhanh (~triệu lần/giây) |

### 2.3 Tại sao KHÔNG nên dùng MD5 cho mật khẩu?

#### ❌ Lý do 1: Quá nhanh → dễ bị Brute Force

```
MD5 có thể hash hàng TỈ chuỗi/giây trên GPU hiện đại.
→ Hacker thử tất cả mật khẩu 6 ký tự chỉ trong vài GIÂY.
```

#### ❌ Lý do 2: Không có Salt → Rainbow Table Attack

Vì MD5 **deterministic** (cùng input = cùng output), hacker chỉ cần tra bảng:

```
Rainbow Table (bảng hash dựng sẵn):
  "123456"   → e10adc3949ba59abbe56e057f20f883e  ← Tìm thấy!
  "password" → 5f4dcc3b5aa765d61d8327deb882cf99
  "admin"    → 21232f297a57a5a743894a0e4a801fc3
  ...hàng tỉ dòng...
```

> Hacker không cần "bẻ khoá" – chỉ cần **tra bảng** là biết password gốc.

#### ❌ Lý do 3: Đã bị "broken" về mặt mật mã học

MD5 đã có **collision attack** (tìm được 2 input khác nhau cho cùng 1 hash) từ năm 2004. Dù không trực tiếp ảnh hưởng password, nó cho thấy MD5 **không an toàn** cho bất kỳ mục đích bảo mật nào.

### 2.4 Minh hoạ bằng Java

```java
import java.security.MessageDigest;

public class MD5Demo {
    public static void main(String[] args) throws Exception {
        String password = "123456";

        // Hash bằng MD5
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(password.getBytes());

        // Chuyển sang hex string
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }

        System.out.println("MD5 hash: " + sb.toString());
        // Output: e10adc3949ba59abbe56e057f20f883e

        // ⚠️ Hash lại lần nữa → CÙNG kết quả (deterministic)
        byte[] digest2 = md.digest(password.getBytes());
        StringBuilder sb2 = new StringBuilder();
        for (byte b : digest2) {
            sb2.append(String.format("%02x", b));
        }
        System.out.println("MD5 hash lần 2: " + sb2.toString());
        // Output: e10adc3949ba59abbe56e057f20f883e ← GIỐNG HỆT!
    }
}
```

> ⚠️ **Kết luận:** KHÔNG BAO GIỜ dùng MD5 để hash mật khẩu. Chỉ nên dùng MD5 cho checksum file (kiểm tra tính toàn vẹn dữ liệu).

---

## 3) BCrypt – Mã hóa mật khẩu an toàn

### 3.1 BCrypt là gì?

**BCrypt** là thuật toán hash được thiết kế **chuyên biệt cho mật khẩu**, khắc phục tất cả nhược điểm của MD5.

### 3.2 So sánh MD5 vs BCrypt

| Tiêu chí | MD5 | BCrypt |
|---|---|---|
| **Tốc độ** | Cực nhanh (~tỉ hash/s) | **Cố tình chậm** (~10 hash/s) |
| **Salt** | ❌ Không có | ✅ Tự động thêm salt ngẫu nhiên |
| **Deterministic** | Cùng input = cùng output | Cùng input = **output KHÁC nhau** mỗi lần |
| **Cost factor** | Không có | Có thể tăng để chậm hơn theo thời gian |
| **An toàn cho password** | ❌ Không | ✅ Có |

### 3.3 BCrypt hoạt động như thế nào?

```
Lần hash 1: BCrypt("123456") → "$2a$10$N9qo8uLOickgx2ZMRZoMye1UqH.7fF0q1vT2hT2vL7FwcB7Lqge.."
Lần hash 2: BCrypt("123456") → "$2a$10$T5KcF7rxIbQH3oWf1ynB5e2D2K5jF.qT3Z0u4L8m9R.cN6.w2pXKe"
                                 ↑              ↑                         ↑
                                 │              │                         │
                              Version      Random Salt              Hash result
                              ($2a$)       (22 ký tự)              (phần còn lại)

→ Cùng password "123456" nhưng hash ra 2 chuỗi KHÁC NHAU!
→ BCrypt vẫn verify đúng vì salt được nhúng trong hash.
```

### 3.4 Cấu trúc chuỗi BCrypt

```
$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
 ├─┤├┤├──────────────────────┤├──────────────────────────────────┤
  │  │           │                           │
  │  │     Salt (22 chars)           Hash (31 chars)
  │  │
  │  Cost factor (2^10 = 1024 rounds)
  │
  Version ($2a$)
```

| Thành phần | Ý nghĩa |
|---|---|
| `$2a$` | Phiên bản thuật toán BCrypt |
| `10` | Cost factor – `2^10 = 1024` vòng lặp. Tăng 1 = **chậm gấp đôi** |
| Salt | 22 ký tự ngẫu nhiên, tự động sinh mỗi lần hash |
| Hash | Kết quả hash cuối cùng |

### 3.5 Minh hoạ bằng Spring Boot

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptDemo {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String password = "123456";

        // Hash 2 lần → ra 2 chuỗi KHÁC nhau
        String hash1 = encoder.encode(password);
        String hash2 = encoder.encode(password);

        System.out.println("Hash 1: " + hash1);
        // $2a$10$N9qo8uLOickgx2ZMRZoMye...
        System.out.println("Hash 2: " + hash2);
        // $2a$10$T5KcF7rxIbQH3oWf1ynB5e...  ← KHÁC!

        // Nhưng verify đều đúng!
        System.out.println("Verify hash1: " + encoder.matches(password, hash1)); // true
        System.out.println("Verify hash2: " + encoder.matches(password, hash2)); // true

        // Sai password → false
        System.out.println("Verify wrong: " + encoder.matches("wrong", hash1)); // false
    }
}
```

> 💡 **Trong dự án của chúng ta**, `BCryptPasswordEncoder` đã được khai báo ở `SecurityConfig` (Buổi 12). Spring Security **tự động** dùng BCrypt khi `AuthenticationManager.authenticate()` so sánh password.

---

## 4) Khái niệm và cấu trúc JWT

Ở Buổi 12, chúng ta đã dùng JWT để xác thực và thấy nó hoạt động (tạo token, gửi trong header, parse trong filter). Bây giờ hãy đi sâu vào **bên trong** JWT để hiểu tại sao nó an toàn – và cũng để hiểu hạn chế của nó (dẫn đến Refresh Token ở phần sau).

### 4.1 JWT là gì?

> **JWT (JSON Web Token)** là chuẩn mở (RFC 7519) để truyền thông tin bảo mật giữa các bên dưới dạng **JSON object được ký số (signed)**.

JWT **không mã hoá** dữ liệu – ai cũng có thể đọc nội dung (decode Base64). JWT chỉ đảm bảo **dữ liệu không bị thay đổi** (integrity) nhờ chữ ký.

### 4.2 Cấu trúc JWT – Ba phần

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIn0.abc123signature
└──────── Header ──────┘└────────────────── Payload ──────────────────┘└── Signature ──┘
```

Mỗi phần được **Base64URL encode** và nối bằng dấu chấm (`.`):

#### Phần 1: Header

```json
{
  "alg": "HS256",    // Thuật toán ký: HMAC-SHA256
  "typ": "JWT"       // Loại token
}
```

#### Phần 2: Payload (Claims)

```json
{
  "sub": "admin",              // Subject – username
  "role": "ROLE_ADMIN",        // Custom claim – role
  "iat": 1712345678,           // Issued At – thời điểm tạo (Unix timestamp)
  "exp": 1712432078            // Expiration – thời điểm hết hạn
}
```

**Phân loại Claims:**

| Loại | Ví dụ | Mô tả |
|---|---|---|
| **Registered** | `sub`, `exp`, `iat`, `iss` | Được định nghĩa sẵn trong RFC 7519 |
| **Public** | `email`, `name` | Tên tự do, nên đăng ký tại IANA |
| **Private** | `role`, `department` | Tuỳ ứng dụng tự định nghĩa |

#### Phần 3: Signature

```
HMAC-SHA256(
  base64UrlEncode(Header) + "." + base64UrlEncode(Payload),
  secretKey
)
```

> **Signature** đảm bảo: nếu ai đó sửa payload (ví dụ đổi `role` từ `USER` thành `ADMIN`), chữ ký sẽ **không khớp** → token bị reject.

### 4.3 Luồng xác thực JWT trong Spring Boot

```
┌─────────┐                          ┌──────────────────┐
│  Client  │    1. POST /auth/login   │   Spring Boot    │
│ (Browser │ ───────────────────────► │                  │
│  / App)  │    {user, password}      │  2. Verify creds │
│          │                          │  3. Generate JWT │
│          │ ◄─────────────────────── │                  │
│          │    { token: "eyJ..." }   │                  │
│          │                          │                  │
│          │    4. GET /api/students   │                  │
│          │    Authorization:         │                  │
│          │    Bearer eyJ...          │                  │
│          │ ───────────────────────► │  5. JwtFilter:   │
│          │                          │     Parse token  │
│          │                          │     Verify sig   │
│          │                          │     Set auth     │
│          │ ◄─────────────────────── │  6. Return data  │
│          │    { data: [...] }       │                  │
└─────────┘                          └──────────────────┘
```

---

## 5) Hạn chế của Access Token đơn lẻ

Hệ thống JWT từ Buổi 12 đã hoạt động tốt với các component:

| Component | File | Chức năng |
|---|---|---|
| `JwtUtil` | `util/JwtUtil.java` | Tạo token, parse, validate |
| `JwtAuthenticationFilter` | `configs/JwtAuthenticationFilter.java` | Xác thực JWT mỗi request |
| `SecurityConfig` | `configs/SecurityConfig.java` | Cấu hình filter chain, phân quyền URL |
| `AuthController` | `controller/auth/AuthController.java` | API `/api/v1/auth/login` |

Nhưng hệ thống này có **một vấn đề lớn** – Access Token có hạn sử dụng:

```yaml
# application.yml (Buổi 12)
jwt:
  expiration-ms: 86400000  # 24 giờ
```

**Kịch bản thực tế:**

```
Tình huống 1: Token hết hạn
  User đăng nhập lúc 8:00 → Token hết hạn lúc 8:00 ngày hôm sau
  → User bị "đá" ra, phải đăng nhập lại
  → Trải nghiệm kém (đặc biệt với mobile app)

Tình huống 2: Tăng thời gian sống token lên 30 ngày
  → Nếu token bị lộ (XSS, log file, screenshot...)
  → Hacker có 30 ngày để dùng token đó
  → Cực kỳ nguy hiểm!

→ Cần giải pháp: Token ngắn hạn + cơ chế "gia hạn" tự động
→ Đó chính là REFRESH TOKEN!
```

---

## 6) Refresh Token

### 6.1 Refresh Token là gì?

> **Refresh Token** là token đặc biệt, dùng duy nhất để **lấy Access Token mới** mà không cần user đăng nhập lại.

### 6.2 So sánh Access Token vs Refresh Token

| Tiêu chí | Access Token | Refresh Token |
|---|---|---|
| **Mục đích** | Truy cập API | Lấy Access Token mới |
| **Thời gian sống** | Ngắn (15-60 phút) | Dài (7-30 ngày) |
| **Lưu ở đâu** | Memory / LocalStorage | HttpOnly Cookie / DB |
| **Gửi khi nào** | Mỗi API request | Chỉ khi Access Token hết hạn |
| **Bị lộ thì sao** | Thiệt hại nhỏ (hết hạn nhanh) | Thiệt hại lớn (cần revoke ngay) |

### 6.3 Luồng hoạt động Refresh Token

```
Client                                Server
  │                                      │
  │  1. POST /auth/login                 │
  │  {username, password}                │
  │ ────────────────────────────────────►│
  │                                      │  Tạo Access Token (15 phút)
  │                                      │  Tạo Refresh Token (7 ngày)
  │                                      │  Lưu Refresh Token vào DB
  │  ◄────────────────────────────────── │
  │  { accessToken, refreshToken }       │
  │                                      │
  │  2. GET /api/students                │
  │  Authorization: Bearer <accessToken> │
  │ ────────────────────────────────────►│  ✅ Token hợp lệ
  │  ◄────────────────────────────────── │
  │  { data: [...] }                     │
  │                                      │
  │  ... 15 phút sau: Access Token hết hạn ...
  │                                      │
  │  3. GET /api/students                │
  │  Authorization: Bearer <accessToken> │
  │ ────────────────────────────────────►│  ❌ 401 Token expired
  │  ◄────────────────────────────────── │
  │  { error: "Token expired" }          │
  │                                      │
  │  4. POST /auth/refresh               │
  │  { refreshToken: "abc..." }          │
  │ ────────────────────────────────────►│  Kiểm tra Refresh Token trong DB
  │                                      │  ✅ Hợp lệ + chưa hết hạn
  │                                      │  Tạo Access Token MỚI
  │  ◄────────────────────────────────── │
  │  { accessToken: "eyJ...(mới)" }     │
  │                                      │
  │  5. GET /api/students (retry)        │
  │  Authorization: Bearer <newToken>    │
  │ ────────────────────────────────────►│  ✅ OK
  │  ◄────────────────────────────────── │
  │  { data: [...] }                     │
```

> 💡 **Tại sao lưu Refresh Token vào DB?**
> - Khác với Access Token (chỉ verify bằng secret key), Refresh Token cần lưu trong DB để có thể **revoke** (vô hiệu hoá) khi cần.
> - Khi user logout, xoá Refresh Token khỏi DB → token cũ không thể dùng lại.

---

## 7) Tạo và quản lý Refresh Token

### 7.1 Entity RefreshToken

```java
package student.management.api_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "refresh_tokens", schema = "app")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true)
    String token;

    @Column(name = "expiry_date", nullable = false)
    Instant expiryDate;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    Boolean isRevoked = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}
```

### 7.2 Tạo bảng trên Database

```sql
-- Migration: Tạo bảng refresh_tokens
CREATE TABLE app.refresh_tokens (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    user_id UUID NOT NULL,
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES app.users(id)
        ON DELETE CASCADE
);

-- Index để tìm kiếm nhanh theo token
CREATE INDEX idx_refresh_token_token ON app.refresh_tokens(token);

-- Index để tìm theo user_id (phục vụ logout + revoke)
CREATE INDEX idx_refresh_token_user_id ON app.refresh_tokens(user_id);
```

### 7.3 RefreshTokenRepository

```java
package student.management.api_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import student.management.api_app.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    // Revoke tất cả refresh token của 1 user (dùng khi logout)
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.isRevoked = true WHERE rt.user.id = :userId")
    void revokeAllByUserId(UUID userId);

    // Xoá các token đã hết hạn (cleanup job)
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < CURRENT_TIMESTAMP")
    void deleteAllExpired();
}
```

### 7.4 RefreshTokenService

```java
package student.management.api_app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import student.management.api_app.model.RefreshToken;
import student.management.api_app.model.User;
import student.management.api_app.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /**
     * Tạo Refresh Token mới cho user
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())      // Token ngẫu nhiên (không phải JWT)
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .isRevoked(false)
                .user(user)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Xác thực Refresh Token
     * - Tìm trong DB
     * - Kiểm tra chưa bị revoke
     * - Kiểm tra chưa hết hạn
     */
    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token không tồn tại"));

        if (refreshToken.getIsRevoked()) {
            throw new RuntimeException("Refresh token đã bị thu hồi");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            // Token hết hạn → xoá luôn khỏi DB
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token đã hết hạn. Vui lòng đăng nhập lại");
        }

        return refreshToken;
    }

    /**
     * Revoke tất cả Refresh Token của 1 user (dùng khi logout)
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
```

### 7.5 Cập nhật `application.yml`

```yaml
jwt:
  secret-key: "my-super-secret-key-for-jwt-must-be-at-least-256-bits-long-1234567890"
  expiration-ms: 900000            # Access Token: 15 phút = 900000ms
  refresh-expiration-ms: 604800000 # Refresh Token: 7 ngày = 604800000ms
```

> 💡 **Lưu ý:** Ở Buổi 12, `expiration-ms` là 24 giờ. Nay giảm xuống **15 phút** vì đã có Refresh Token "backup".

---

## 8) Triển khai Refresh Token với JWT

### 8.1 Cập nhật DTO

```java
// === AuthResponse.java (CẬP NHẬT) ===
package student.management.api_app.dto.auth;

public record AuthResponse(
    String accessToken,        // ← Đổi tên từ "token" thành "accessToken"
    String refreshToken,       // ← THÊM MỚI
    String username,
    String role
) {}
```

```java
// === RefreshTokenRequest.java (MỚI) ===
package student.management.api_app.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
    @NotBlank(message = "Refresh token không được để trống")
    String refreshToken
) {}
```

### 8.2 Cập nhật AuthController

```java
package student.management.api_app.controller.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import student.management.api_app.dto.auth.AuthRequest;
import student.management.api_app.dto.auth.AuthResponse;
import student.management.api_app.dto.auth.RefreshTokenRequest;
import student.management.api_app.model.RefreshToken;
import student.management.api_app.model.User;
import student.management.api_app.service.impl.RefreshTokenService;
import student.management.api_app.util.JwtUtil;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * Đăng nhập – trả về cả Access Token và Refresh Token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for user: {}", request.username());

        // 1. Xác thực username & password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // 2. Lấy User entity
        User user = (User) authentication.getPrincipal();

        // 3. Tạo Access Token (JWT)
        String accessToken = jwtUtil.generateToken(user);

        // 4. Tạo Refresh Token (lưu vào DB)
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        // 5. Trả về response
        String role = user.getAuthorities().iterator().next().getAuthority();
        log.info("Login successful for user: {}, role: {}", user.getUsername(), role);

        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user.getUsername(),
                role
        ));
    }

    /**
     * Refresh – Tạo Access Token mới từ Refresh Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.info("Token refresh attempt");

        // 1. Verify refresh token (kiểm tra DB + hạn)
        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(
                request.refreshToken()
        );

        // 2. Lấy user từ refresh token
        User user = refreshToken.getUser();

        // 3. Tạo Access Token MỚI
        String newAccessToken = jwtUtil.generateToken(user);

        // 4. Trả về token mới (giữ nguyên refresh token cũ)
        String role = user.getAuthorities().iterator().next().getAuthority();

        return ResponseEntity.ok(new AuthResponse(
                newAccessToken,
                refreshToken.getToken(),    // Giữ nguyên refresh token
                user.getUsername(),
                role
        ));
    }
}
```

### 8.3 Luồng hoàn chỉnh với cả hai token

```
POST /api/v1/auth/login
  Request:  { "username": "admin", "password": "123456" }
  Response: {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",     ← Dùng để gọi API (15 phút)
    "refreshToken": "c5f0e8a2-3b4d-4e6f-8a9b...", ← Dùng để lấy token mới (7 ngày)
    "username": "admin",
    "role": "ROLE_ADMIN"
  }

--- Access Token hết hạn ---

POST /api/v1/auth/refresh
  Request:  { "refreshToken": "c5f0e8a2-3b4d-4e6f-8a9b..." }
  Response: {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...(MỚI)",
    "refreshToken": "c5f0e8a2-3b4d-4e6f-8a9b...",  ← giữ nguyên
    "username": "admin",
    "role": "ROLE_ADMIN"
  }
```

---

## 9) Cấu hình Logout trong Spring Security

### 9.1 Logout trong hệ thống JWT hoạt động thế nào?

Không giống session-based (chỉ cần xoá session), JWT là **stateless** → server không thể "xoá" một token. Chiến lược Logout:

```
┌──────────────────────────────────────────────────────┐
│                    LOGOUT STRATEGY                    │
├──────────────────────────────────────────────────────┤
│                                                      │
│  1. Revoke Refresh Token (chặn gia hạn)             │
│     → Xoá/đánh dấu revoked trong DB                 │
│     → User không thể lấy Access Token mới           │
│                                                      │
│  2. Access Token hiện tại                            │
│     → Vẫn còn hoạt động cho đến khi hết hạn         │
│     → Nhưng chỉ còn tối đa 15 phút (nếu cấu hình   │
│       expiration-ms = 900000)                        │
│     → Rủi ro chấp nhận được                         │
│                                                      │
│  3. (Tuỳ chọn) Token Blacklist                       │
│     → Lưu Access Token vào blacklist (Redis/DB)      │
│     → JwtFilter kiểm tra blacklist mỗi request       │
│     → Logout tức thì, nhưng thêm complexity          │
│                                                      │
└──────────────────────────────────────────────────────┘
```

> Trong bài học này, chúng ta dùng **Chiến lược 1** (Revoke Refresh Token) – đơn giản và đủ an toàn cho hầu hết ứng dụng.

### 9.2 API Logout

Thêm endpoint logout vào `AuthController`.

> 💡 **Tại sao dùng `SecurityContextHolder` thay vì parse header thủ công?**
>
> Khi request đến endpoint `/logout`, `JwtAuthenticationFilter` đã chạy trước đó → user đã được set vào `SecurityContext`. Ta chỉ cần **lấy ra** – không cần parse token lại lần nữa. Điều này **nhất quán** với cách Spring Security hoạt động.

```java
// Thêm import vào đầu file AuthController
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import student.management.api_app.dto.AppResponse;

// Thêm endpoint này vào AuthController
@PostMapping("/logout")
public ResponseEntity<AppResponse<Void>> logout(
        @AuthenticationPrincipal User currentUser) {

    log.info("Logout request for user: {}", currentUser.getUsername());

    // Revoke TẤT CẢ refresh token của user
    refreshTokenService.revokeAllUserTokens(currentUser.getId());

    log.info("Logout successful for user: {}", currentUser.getUsername());

    return ResponseEntity.ok(
            AppResponse.<Void>builder()
                    .success(true)
                    .build()
    );
}
```

**Giải thích `@AuthenticationPrincipal`:**

| Cách cũ (thủ công) | Cách mới (annotation) |
|---|---|
| Tự đọc `Authorization` header | Spring tự inject |
| Tự gọi `jwtUtil.extractUsername(token)` | Không cần |
| Tự gọi `userDetailsService.loadUserByUsername(...)` | Không cần |
| Nhiều code, dễ sai | **1 annotation, an toàn** |

> Vì `User` entity của chúng ta implement `UserDetails`, Spring Security tự động cast `SecurityContext.getAuthentication().getPrincipal()` thành `User` và inject vào parameter.

> ⚠️ **Lưu ý:** Endpoint `/api/v1/auth/logout` cần user **đã đăng nhập** (có token). Nhưng nó vẫn nằm trong URL pattern `/api/v1/auth/**` đã được `permitAll()` ở `SecurityConfig`. Do đó, cần cập nhật SecurityConfig (xem section 10).

---

## 10) Tích hợp Logout và bảo mật JWT

### 10.1 Cập nhật SecurityConfig

```java
// Cập nhật SecurityConfig.securityFilterChain()
.authorizeHttpRequests(auth -> auth
    // Auth endpoints: login & refresh cho phép public, logout yêu cầu đăng nhập
    .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
    .requestMatchers("/api/v1/auth/logout").authenticated()  // ← THÊM MỚI

    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/actuator/**").permitAll()

    // ... giữ nguyên phần còn lại ...
)
```

> **Trước đó** ta dùng `.requestMatchers("/api/v1/auth/**").permitAll()` – cho phép TẤT CẢ auth endpoint. Giờ cần tách riêng vì `/logout` phải authenticated.

### 10.2 Xử lý Exception cho Refresh Token

Thêm vào `GlobalExceptionHandler`:

```java
// Bắt lỗi liên quan đến Refresh Token
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<AppResponse<Void>> handleRefreshTokenException(
        RuntimeException ex,
        HttpServletRequest request) {

    // Chỉ bắt các lỗi liên quan đến refresh token
    if (ex.getMessage() != null && (
            ex.getMessage().contains("Refresh token") ||
            ex.getMessage().contains("refresh token"))) {

        log.warn("Refresh token error at [{}]: {}",
                request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                AppResponse.<Void>builder()
                        .success(false)
                        .error(AppResponse.AppError.builder()
                                .code("REFRESH_TOKEN_INVALID")
                                .message(ex.getMessage())
                                .path(request.getRequestURI())
                                .build())
                        .build()
        );
    }

    // Các RuntimeException khác → 500
    throw ex;
}
```

> 💡 **Cải tiến tốt hơn:** Thay vì dùng `RuntimeException` chung, hãy tạo custom exception `InvalidRefreshTokenException extends RuntimeException` để phân biệt rõ ràng hơn (xem Bài tập nâng cao).

### 10.3 Tổng quan luồng hoàn chỉnh

```
┌────────────────────────────────────────────────────────────────────┐
│                  AUTHENTICATION FLOW HOÀN CHỈNH                    │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  LOGIN ──────────────────────────────────────────────────────┐     │
│  POST /auth/login {user, pass}                               │     │
│    → Verify credentials                                      │     │
│    → Generate Access Token (15 min)                          │     │
│    → Generate Refresh Token (7 days, save to DB)             │     │
│    → Return { accessToken, refreshToken, username, role }    │     │
│                                                              │     │
│  API CALLS ─────────────────────────────────────────────────►│     │
│  GET/POST/PUT/DELETE /api/v1/**                              │     │
│  Header: Authorization: Bearer <accessToken>                 │     │
│    → JwtFilter parse & verify                                │     │
│    → Set SecurityContext                                     │     │
│    → Controller handles request                              │     │
│                                                              │     │
│  REFRESH ────────────────────────────────────────────────────│     │
│  POST /auth/refresh { refreshToken }                         │     │
│    → Verify refreshToken in DB (not revoked, not expired)    │     │
│    → Generate NEW Access Token                               │     │
│    → Return { newAccessToken, refreshToken }                 │     │
│                                                              │     │
│  LOGOUT ─────────────────────────────────────────────────────│     │
│  POST /auth/logout                                           │     │
│  Header: Authorization: Bearer <accessToken>                 │     │
│    → Extract user from token                                 │     │
│    → Revoke ALL refresh tokens in DB                         │     │
│    → Access Token still valid until expiry (max 15 min)      │     │
│                                                              │     │
└──────────────────────────────────────────────────────────────┘     │
                                                                     │
└────────────────────────────────────────────────────────────────────┘
```

### 10.4 Bảo mật Refresh Token phía Client

Chúng ta đã xử lý xong phía **server**. Nhưng bảo mật token cũng phụ thuộc vào cách **client (frontend)** lưu trữ chúng:

| Nơi lưu | Access Token | Refresh Token | Mức an toàn |
|---|---|---|---|
| **Memory (biến JS)** | ✅ Tốt nhất | ❌ Mất khi refresh page | 🟢 Cao |
| **HttpOnly Cookie** | Không phổ biến | ✅ Tốt nhất | 🟢 Cao (XSS không đọc được) |
| **SessionStorage** | Chấp nhận được | ⚠️ Tạm được | 🟡 Trung bình |
| **LocalStorage** | ⚠️ Rủi ro XSS | ❌ Nguy hiểm | 🔴 Thấp |

**Khuyến nghị cho dự án thực tế:**

```
┌─────────────────────────────────────────────────┐
│  Access Token  → Lưu trong Memory (biến JS)     │
│  Refresh Token → Lưu trong HttpOnly Cookie       │
│                                                  │
│  Lý do:                                          │
│  • Memory: XSS không thể đọc biến JS nội bộ     │
│  • HttpOnly Cookie: JavaScript không truy cập    │
│    được → an toàn trước XSS                      │
│  • Khi refresh page: Access Token mất,           │
│    nhưng tự động gọi /auth/refresh               │
│    (cookie tự gửi kèm) → lấy token mới           │
└─────────────────────────────────────────────────┘
```

> ⚠️ **Trong bài học này**, chúng ta gửi Refresh Token qua JSON body cho đơn giản. Trong dự án production, nên chuyển sang **HttpOnly Cookie** – sẽ được đề cập khi học CORS ở buổi sau.

---

## 11) Lỗi hay gặp

### ❌ Lỗi 1: Dùng MD5 để hash password

```java
// ❌ SAI – MD5 không an toàn, dễ bị tra bảng Rainbow Table
String hashed = DigestUtils.md5Hex(password);

// ✅ ĐÚNG – Dùng BCrypt
String hashed = new BCryptPasswordEncoder().encode(password);
```

### ❌ Lỗi 2: Quên cập nhật `AuthResponse` khi thêm Refresh Token

```java
// ❌ SAI – Response cũ chỉ có 1 token
public record AuthResponse(String token, String username, String role) {}

// ✅ ĐÚNG – Tách rõ Access Token và Refresh Token
public record AuthResponse(String accessToken, String refreshToken, String username, String role) {}
```

> Đổi tên field trong response là **breaking change** – Frontend cần cập nhật theo.

### ❌ Lỗi 3: Quên `@Transactional` khi revoke tokens

```java
// ❌ SAI – UPDATE hàng loạt mà không có @Transactional
public void revokeAllUserTokens(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId); // → TransactionRequiredException
}

// ✅ ĐÚNG
@Transactional
public void revokeAllUserTokens(UUID userId) {
    refreshTokenRepository.revokeAllByUserId(userId);
}
```

### ❌ Lỗi 4: SecurityConfig vẫn dùng `"/api/v1/auth/**"` permitAll sau khi thêm logout

```java
// ❌ SAI – Logout cũng bị permitAll → ai cũng gọi được mà không cần token
.requestMatchers("/api/v1/auth/**").permitAll()

// ✅ ĐÚNG – Tách riêng
.requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
.requestMatchers("/api/v1/auth/logout").authenticated()
```

### ❌ Lỗi 5: Không xoá Refresh Token khi user bị khoá (isActive = false)

```java
// ❌ SAI – Chỉ khoá user nhưng Refresh Token vẫn còn trong DB
// → User vẫn có thể gọi /auth/refresh để lấy Access Token mới!

// ✅ ĐÚNG – Khi khoá user, revoke luôn tất cả Refresh Token
@Transactional
public void deactivateUser(UUID userId) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
    user.setIsActive(false);
    userRepository.save(user);

    // Revoke tất cả Refresh Token
    refreshTokenService.revokeAllUserTokens(userId);
}
```

---

## 12) Tổng kết Buổi 13

### 13.1 Những gì đã học

| # | Nội dung | Ghi chú |
|---|---|---|
| 1 | Authentication vs Authorization | Hai khái niệm nền tảng: xác thực (ai?) và phân quyền (được làm gì?) |
| 2 | MD5 và lý do không nên dùng | Quá nhanh, không có salt, đã bị broken |
| 3 | BCrypt | Salt ngẫu nhiên, cost factor, cố tình chậm, an toàn cho password |
| 4 | JWT cấu trúc | Header + Payload + Signature, Base64URL encoded |
| 5 | Refresh Token | Token dài hạn lưu DB, dùng để gia hạn Access Token |
| 6 | Triển khai Refresh Token | Entity, Repository, Service, API `/auth/refresh` |
| 7 | Logout | Revoke tất cả Refresh Token của user |
| 8 | Tích hợp bảo mật | Cập nhật SecurityConfig, Exception handling |

### 13.2 Checklist công việc

- [ ] Hiểu phân biệt Authentication vs Authorization.
- [ ] Hiểu tại sao MD5 không an toàn và BCrypt an toàn cho password.
- [ ] Hiểu cấu trúc JWT (Header, Payload, Signature).
- [ ] Tạo Entity `RefreshToken` và bảng `refresh_tokens` trên DB.
- [ ] Tạo `RefreshTokenRepository` với các query cần thiết.
- [ ] Tạo `RefreshTokenService` (create, verify, revoke).
- [ ] Cập nhật `application.yml` với refresh token expiration.
- [ ] Cập nhật `AuthResponse` thêm field `refreshToken`.
- [ ] Cập nhật `AuthController`: thêm `/auth/refresh` và `/auth/logout`.
- [ ] Cập nhật `SecurityConfig`: tách riêng permission cho login/refresh/logout.
- [ ] Test toàn bộ flow: login → gọi API → token hết hạn → refresh → logout.

---

## 13) Bài tập

### Bài tập 1: Triển khai Refresh Token (⭐ Bắt buộc)

**Yêu cầu:**
1. Tạo đầy đủ các class: `RefreshToken` entity, `RefreshTokenRepository`, `RefreshTokenService`.
2. Tạo bảng `refresh_tokens` trên database (dùng migration hoặc SQL thủ công).
3. Cập nhật `AuthResponse` thêm field `refreshToken`.
4. Cập nhật `AuthController` với 2 endpoint mới: `/auth/refresh` và `/auth/logout`.
5. Cập nhật `application.yml`: giảm `expiration-ms` xuống 15 phút, thêm `refresh-expiration-ms` = 7 ngày.

**Test bằng Postman:**
- `POST /api/v1/auth/login` → nhận được cả `accessToken` và `refreshToken`.
- `POST /api/v1/auth/refresh` với `refreshToken` → nhận được `accessToken` mới.
- `POST /api/v1/auth/logout` (có Bearer token) → thành công.
- `POST /api/v1/auth/refresh` lại (sau logout) → nhận lỗi "Refresh token đã bị thu hồi".

---

### Bài tập 2: Cập nhật SecurityConfig (⭐ Bắt buộc)

**Yêu cầu:**
1. Sửa SecurityConfig: tách `/api/v1/auth/login` và `/api/v1/auth/refresh` thành `permitAll()` riêng.
2. Đặt `/api/v1/auth/logout` là `authenticated()`.
3. Test:
   - Gọi `/auth/logout` không có token → nhận 401 Unauthorized.
   - Gọi `/auth/logout` có token hợp lệ → thành công.

---

### Bài tập 3: Custom Exception cho Refresh Token (⭐⭐ Nâng cao)

**Yêu cầu:**
1. Tạo custom exception `InvalidRefreshTokenException extends RuntimeException`.
2. Sửa `RefreshTokenService` dùng `InvalidRefreshTokenException` thay vì `RuntimeException`.
3. Thêm handler trong `GlobalExceptionHandler` để bắt `InvalidRefreshTokenException` → trả về 401 với message phù hợp.

---

### Bài tập 4: Token Rotation (⭐⭐ Nâng cao)

**Yêu cầu:**

Hiện tại khi gọi `/auth/refresh`, server trả về Access Token mới nhưng **giữ nguyên** Refresh Token cũ. Hãy triển khai **Token Rotation**:

1. Mỗi lần gọi `/auth/refresh`:
   - Revoke Refresh Token cũ.
   - Tạo Refresh Token MỚI.
   - Trả về cả Access Token mới và Refresh Token mới.
2. Ưu điểm: Nếu Refresh Token bị lộ và hacker dùng trước → Refresh Token cũ bị revoke → user gốc gọi refresh sẽ thất bại → **phát hiện** bị tấn công.

---

### Bài tập 5: Cleanup Expired Tokens (⭐⭐⭐ Nâng cao)

**Yêu cầu:**

Mỗi khi user đăng nhập lại, một Refresh Token mới được tạo. Theo thời gian, bảng `refresh_tokens` sẽ phình to với các token hết hạn.

1. Sử dụng `@Scheduled` (Spring Scheduling) để tạo job chạy mỗi ngày lúc 2:00 AM.
2. Job này gọi `refreshTokenRepository.deleteAllExpired()` để xoá các token đã hết hạn.
3. Bật scheduling bằng `@EnableScheduling` trên Application class.

> 💡 Hint:
> ```java
> @Scheduled(cron = "0 0 2 * * *") // Chạy lúc 2:00 AM mỗi ngày
> @Transactional
> public void cleanupExpiredTokens() { ... }
> ```

---

**Chuẩn bị cho Buổi 14:**
Ở buổi tiếp theo, chúng ta sẽ tìm hiểu về **CORS Configuration** cho phép Frontend (React/Angular) gọi API an toàn, **API Versioning**, và các best practices khi deploy Spring Boot application.
