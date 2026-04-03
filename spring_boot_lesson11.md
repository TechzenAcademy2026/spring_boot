# Spring Boot – Buổi 11: Code Quality với SonarLint/SonarQube & Giới thiệu Security

## 1) Tổng quan về Code Quality (Chất lượng mã nguồn)

### 1.1 Code Quality là gì?
Code Quality (chất lượng mã nguồn) là thước đo đánh giá mức độ sạch, dễ đọc, dễ bảo trì và an toàn của mã nguồn. Một dự án có Code Quality tốt sẽ giúp:
- **Dễ dàng bảo trì (Maintainability):** Code dễ đọc, dễ hiểu, dễ sửa chữa và thêm tính năng mới.
- **Độ tin cậy cao (Reliability):** Ít bug, giảm thiểu rủi ro crash hệ thống trên môi trường Production.
- **Tính bảo mật (Security):** Không chứa các rủi ro bảo mật (vulnerability) dễ bị hacker khai thác.
- **Hiệu suất (Efficiency):** Code chạy tối ưu, không tiêu tốn tài nguyên thừa thãi.

### 1.2 Tại sao cần công cụ phân tích tĩnh (Static Code Analysis)?
Dù có Unit Test (như học ở Buổi 10) hay Review Code chéo giữa các thành viên (Peer Review), con người vẫn có thể bỏ sót những lỗi tiềm ẩn, vi phạm coding convention, hoặc các rủi ro bảo mật sâu bên trong.

Các công cụ phân tích tĩnh quét mã nguồn **mà không cần chạy (execute) nó**, tự động đối chiếu với hàng nghìn quy tắc (rules) chuẩn mực của ngành để phát hiện ra những vấn đề mà mắt thường khó thấy. Trong hệ sinh thái Java/Spring Boot, **SonarLint** và **SonarQube** là bộ đôi công cụ phổ biến nhất.

---

## 2) SonarLint: Kiểm tra mã nguồn ngay khi gõ code

### 2.1 SonarLint là gì?
- **Định nghĩa:** SonarLint là một IDE Extension/Plugin (hỗ trợ IntelliJ IDEA, Eclipse, VS Code,...).
- **Vai trò:** Hoạt động như một công cụ kiểm tra lỗi chính tả nhưng dành cho code. Nó phân tích code **ngay lập tức (on the fly)** trong lúc bạn đang gõ và gạch dưới những đoạn code có vấn đề.
- **Lợi ích:** Giúp lập trình viên fix lỗi ngay trước khi commit code (shift-left testing).

### 2.2 Cài đặt SonarLint trên IntelliJ IDEA / Eclipse / VS Code
- **IntelliJ IDEA:** `File` > `Settings` > `Plugins` > Tìm kiếm: `SonarLint` > Install > Restart IDE.
- **VS Code:** Trình đơn `Extensions` (Ctrl+Shift+X) > Tìm kiếm: `SonarLint` > Install.

### 2.3 Các loại vấn đề SonarLint thường phát hiện (Issues)

Sonar tóm gọn các vấn đề thành 3 nhóm tính chất chính:
1. **Bug (Lỗi):** Một đoạn code chắc chắn sẽ gây ra lỗi mạch logic hoặc throw Exception ở runtime (VD: NullPointerException).
2. **Vulnerability (Lỗ hổng bảo mật):** Lỗ hổng có thể bị hacker khai thác (VD: SQL Injection, Hardcode password).
3. **Code Smell (Mùi code):** Code hoạt động được nhưng khó đọc, khó bảo trì, vi phạm clean code (VD: method quá dài, vòng lặp lồng nhau quá sâu, biến không bao giờ được sử dụng).

**Ví dụ một Bug mà SonarLint phát hiện ngay lập tức:**
```java
// ❌ SonarLint sẽ gạch dưới màu đỏ tía (Bug)
String name = null;
if (name.length() > 0) { // Sẽ văng NullPointerException ở đây
    System.out.println("Tên không rỗng");
}

// ✅ ĐÚNG: Sửa lại theo gợi ý
if (name != null && name.length() > 0) {
    System.out.println("Tên không rỗng");
}
```

---

## 3) SonarQube: Trung tâm phân tích chất lượng của dự án

### 3.1 SonarQube khác gì SonarLint?
- **SonarLint:** Dùng ở máy cá nhân (Local) của từng Dev. Giúp bắt lỗi trong lúc gõ.
- **SonarQube:** Là một Server tập trung (Web Dashboard). Nó quét toàn bộ dự án từ nhánh chính (thường tích hợp vào quy trình CI/CD như Jenkins, GitHub Actions, GitLab CI) và cung cấp báo cáo tổng quan cho toàn bộ Team, bao gồm cả độ bao phủ của Test (Code Coverage).

### 3.2 Các chỉ số quan trọng (Metrics) trên SonarQube

Khi xem báo cáo trên SonarQube dashboard, bạn sẽ quan tâm đến các chỉ số sau:
- **Quality Gate:** Cánh cổng chất lượng (Passed / Failed). Ví dụ cấu hình: Cứ có 1 Blocker Bug là tạch Quality Gate, không cho phép deploy lên Production.
- **Bugs, Vulnerabilities, Code Smells:** Số lượng 이슈 đang tồn đọng trong project được chia theo mức độ nghiêm trọng (Blocker, Critical, Major, Minor, Info).
- **Coverage (Độ bao phủ test):** Tỉ lệ phần trăm code đã được chạy tới bởi Unit Test. (Mức lý tưởng thường là > 80%).
- **Duplication (Trùng lặp):** Phần trăm code bị copy-paste trùng lặp ở nhiều nơi trong dự án. Càng cao thì dự án càng khó bảo trì.
- **Technical Debt (Nợ kỹ thuật):** Ước tính tổng số thời gian (bao nhiêu ngày/giờ) cần thiết để sửa toàn bộ các Code Smells hiện tại trong project.

### 3.3 Cài đặt và cấu hình SonarQube Local (Bằng Docker)

Cách nhanh nhất để chạy SonarQube ở môi trường Local là dùng Docker. Mở Terminal và chạy lệnh sau:
```bash
docker run -d --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:lts-community
```
- Truy cập vào **http://localhost:9000**
- Đăng nhập với tài khoản mặc định: `admin` / mật khẩu: `admin`. Bảng điều khiển sẽ yêu cầu bạn đổi mật khẩu mới.
- **Tạo Project & Token:** Chọn *Create a local project* > Nhập tên Project (Ví dụ: `student-management`) > Chọn *Set Up* > Chọn quy trình *Locally* > Generate một Project Token (Lưu ý copy lại chuỗi Token này để dùng ghép vào Gradle).

### 3.4 Các chú ý (Lưu ý) quan trọng khi cấu hình SonarQube
1. **Yêu cầu phần cứng:** SonarQube chứa sẵn một Embed database (H2 hoặc Postgres) và Elasticsearch, do đó nó khá tốn RAM. Docker container của SonarQube cần ít nhất **2GB RAM** trống để khởi động thành công. Nếu Docker báo lỗi "Exited (137)", nguyên nhân thường do thiếu RAM hoặc cần tăng giới hạn `vm.max_map_count` của hệ điều hành.
2. **Đồng bộ phiên bản Java (JDK):** Khác biệt phiên bản Java giữa máy build (chạy Gradle) và SonarQube có thể gây lỗi. Sonar Scanner hiện tại yêu cầu chạy trên nền **Java 17** hoặc **Java 21**. Code của bạn có thể compile ở Java 8, nhưng bản thân tiến trình (process) chạy Sonar Scanner thì máy bạn phải cài JDK 17+.
3. **Mã Token bị lộ:** Không bao giờ commit mã `sonar.login` (Token) cứng vào file `build.gradle` rồi push lên GitHub. Bất cứ ai có token này có thể dùng API để thao tác vào SonarQube Server của bạn. Nên dùng tham số dòng lệnh hoặc load từ biến môi trường (Environment Variable).
4. **Loại trừ Code (Exclusions):** Các class tự động sinh (Generated Code như DTO, Entity class, các file config) thường không chứa logic nghiệp vụ. Cần cấu hình `sonar.exclusions` để bỏ qua chúng, giúp báo cáo tập trung vào các Service/Controller chính, tránh bị nhiễu.

### 3.5 Tích hợp SonarQube vào Gradle và Chạy phân tích

Để quét dự án Spring Boot và đẩy kết quả lên SonarQube, bạn thêm cấu hình plugin sau vào file `build.gradle`:

```groovy
plugins {
    // ...
    id "org.sonarqube" version "4.0.0.2929"
}

sonar {
    properties {
        property "sonar.projectKey", "student-management"
        property "sonar.projectName", "Student Management API"
        property "sonar.host.url", "http://localhost:9000"
        
        // Config các thư mục không muốn Sonar quét (Tránh báo Code Smell ảo)
        property "sonar.exclusions", "**/dto/**, **/model/**, **/config/**, **/*Exception.java"
        
        // Lưu ý: Không nên hardcode Token thế này trong thực tế, nên đưa vào biến môi trường
        property "sonar.login", "your_token_here" 
    }
}
```

**Lệnh chạy quét:**
Chạy lệnh bên dưới (hoặc truyền truyền trực tiếp Token qua tham số để file gradle sạch sẽ):
```bash
./gradlew test sonar -Dsonar.token="your_token_here"
```
*(Chạy `test` trước để Sonar đo lường được % Code Coverage dự án, sau đó `sonar` sẽ tổng hợp và đẩy báo cáo)*

---

## 4) Cải thiện chất lượng mã từ báo cáo Sonar

Khi đối diện với report hàng trăm Code Smells trên Sonar, tinh thần chung là: **Làm sạch code từ từ thay vì cố sửa hết trong 1 lần**.

### 4.1 Quy tắc "Boy Scout Rule" (Khu cắm trại)
> "Always leave the campground cleaner than you found it."
> (Hãy luôn để lại khu cắm trại sạch sẽ hơn lúc bạn đến).

Trong code, quy tắc này nghĩa là: Thay vì mất 2 tuần chỉ để fix Sonar lints cho code cũ, mỗi lần bạn vào sửa một file hay thêm tính năng mới ở tính năng nào, hãy dọn dẹp các Code smells ở phạm vi khu vực đó. Dần dần Technical Debt sẽ giảm xuống.

### 4.2 Một số Code Smells Spring Boot phổ biến và cách sửa

**1. Field Injection không được khuyến khích:**
```java
// ❌ Code Smell: Khó viết unit test, giấu đi tính phụ thuộc
@Autowired
private StudentRepository repository;

// ✅ Sửa lại: Dùng Constructor Injection (qua @RequiredArgsConstructor của Lombok)
private final StudentRepository repository;
```

**2. Bắt `Exception` chung chung thay vì Exception cụ thể:**
```java
// ❌ Code Smell: Bắt Exception có thể vô tình nuốt cả RuntimeException ngoài ý muốn
try {
    saveData();
} catch (Exception e) {
    log.error("Lỗi", e);
}

// ✅ Sửa lại: Catch đúng exception mong đợi
try {
    saveData();
} catch (DataIntegrityViolationException e) {
    log.error("Lỗi trùng lặp dữ liệu", e);
}
```

**3. Khai báo hằng số không dùng `static final`**
```java
// ❌ Code Smell: Mỗi lần khởi tạo class lại nạp lại chuỗi vào bộ nhớ
private String DEFAULT_ROLE = "USER";

// ✅ Sửa lại: Rõ ràng, bảo toàn bộ nhớ
private static final String DEFAULT_ROLE = "USER";
```

---

## 5) Giới thiệu về Security Intro (Bảo mật cơ bản)

Hệ thống quản lý không chỉ cần chạy đúng, mà còn phải chống lại được các cuộc tấn công. Ở các buổi học sau chúng ta sẽ học sâu về **Spring Security** & **JWT**, nhưng trước hết chúng ta cần làm quen với OWASP và tư duy bảo mật nền tảng.

### 5.1 OWASP Top 10 là gì?
**OWASP** (Open Worldwide Application Security Project) là trang tổ chức phi lợi nhuận hướng dẫn tiêu chuẩn bảo mật phần mềm. Cứ vài năm họ sẽ công bố danh sách "OWASP Top 10" – 10 lỗ hổng bảo mật phổ biến và nguy hiểm nhất đối với ứng dụng Web.

### 5.2 Một số lỗ hổng kinh điển

#### A. SQL Injection (Kích hoạt truy vấn trái phép)
- **Kịch bản:** Người dùng truyền chuỗi độc hại qua Form đăng nhập. Ví dụ username: `admin' OR '1'='1`.
- **Hệ quả:** Mã SQL được ráp nối chuỗi sẽ tự động bypass mật khẩu và chiếm quyền tài khoản.
- **Bảo vệ trong Spring Boot:** Sử dụng **Spring Data JPA/Hibernate**. Hibernate tự động sử dụng `PreparedStatement` để *bind* tham số an toàn thay vì cộng chuỗi trực tiếp.
  ```java
  // ❌ LỖI BẢO MẬT NGHIÊM TRỌNG (cộng chuỗi)
  String query = "SELECT * FROM users WHERE username = '" + username + "'";
  
  // ✅ AN TOÀN (Dùng JPA method)
  Optional<User> user = userRepository.findByUsername(username); 
  ```

#### B. Broken Authentication (Lỗ hổng xác thực)
- **Kịch bản:** Mật khẩu user được lưu thô (plaintext) `/` "123456" trên Database. Hacker dump DB là lấy được tất cả. Session không expire, hay cho phép Brute Force (thử pass tỷ lần).
- **Bảo vệ trong Spring Boot:** 
  - Bắt buộc hash mật khẩu (thường qua **BCryptPasswordEncoder**).
  - Tích hợp cơ chế Rate Limit chặn chống brute force.
  - Sử dụng Token (JWT) an toàn có thời hạn sống ngắn.

#### C. Insecure Direct Object References (IDOR - Đoán định Object)
- **Kịch bản:** Trên Web, link có chứa id thư mục như: `/invoice/download?id=100`. Kẻ tấn công đổi `id=101` trên URL và xem được hoá đơn của người khác.
- **Bảo vệ trong Spring Boot:**
  - Logic trong `Controller` / `Service` phải luôn kiểm tra xem *ID tài nguyên* được request có thuộc quyền sở hữu của *User đang đăng nhập (Current User)* hay không.
  - Không expose bảng ID tự tăng (`Long id`), thay vào đó sử dụng `UUID` cho các endpoint public.

#### D. Cross-Site Scripting (XSS - Chèn mã độc)
- **Kịch bản:** Kẻ tấn công lợi dụng các ô nhập liệu (Text area, comment) để chèn đoạn mã script độc hại (VD: `<script>alert('XSS');</script>`). Khi người dùng khác mở trang chứa comment này, mã script sẽ chạy trên trình duyệt của họ, có thể lấy cắp cookie hoặc session token.
- **Bảo vệ trong Spring Boot & Frontend:**
  - **Backend:** Thường Spring tự động escape dữ liệu JSON qua bộ Serializer (Jackson), tuy nhiên nếu bạn trả về HTML thuần (Thymeleaf/JSP), cần đảm bảo template engine đang tự động escape các biến (Thymeleaf sử dụng `th:text` tự động ngăn chặn XSS).
  - **Frontend (React, Vue, Angular):** Các framework hiện đại hầu hết tự động escape content.
  - Cấu hình HTTP Header an toàn: Sử dụng **Content Security Policy (CSP)** trong Spring Security để chỉ định rõ nguồn nào được phép tải các đoạn mã Script.

---

## 6) Tổng kết Buổi 11

**Checklist công việc cần làm:**
- [ ] Cài đặt SonarLint vào IDE đang dùng (IntelliJ / VS Code).
- [ ] Quét thử một class như `PersonService` và đọc thử cảnh báo (viền vàng/đỏ gạch dưới), click vào gợi ý của SonarLint để xem hướng fix.
- [ ] Refactor mã nguồn loại bỏ Code Smells (ít nhất các mục không cần thiết, unused imports, comment code).
- [ ] Luôn có tư duy phòng thủ (Defensive Programming): Không bao giờ tin tưởng Input của User (Luôn Validation), và để ý quyền sở hữu truy xuất tài nguyên (Authorization/IDOR).

**Chuẩn bị cho Buổi 12 (Spring Security):**
Ở bài tiếp theo, chúng ta sẽ áp dụng lý thuyết Security Intro này vào code thực tế thông qua việc cài đặt module Spring Security, quản lý luồng Xác thực (Authentication) và Phân quyền (Authorization), và cấp phát JSON Web Token (JWT).
