# Spring Boot – Buổi 8: Entity Relationships & Performance

## 1) Tổng quan về quan hệ giữa các Entity

Trong ứng dụng Spring Data JPA, quan hệ giữa các bảng trong database được ánh xạ thành quan hệ giữa các entity tương ứng. 
Đây là nền tảng quan trọng để xây dựng mô hình dữ liệu đúng chuẩn và truy vấn hiệu quả.

> Các loại quan hệ chính:
> * One-to-One (`1–1`)
> * One-to-Many / Many-to-One (`1–N` / `N–1`)
> * Many-to-Many (`N–N`)

---

## 2) One-to-Many & Many-to-One Mapping

### 2.1 Nghiệp vụ 

* Một `Major` (ngành học) có thể có nhiều `Student` → quan hệ One-to-Many từ `Major` đến `Student`
* Một `Student` chỉ thuộc một `Major` → quan hệ Many-to-One từ `Student` đến `Major`
* Học viên dự thính: Một `Student` có thể không thuộc `Major` nào 
* Về mặt DB:
  * Bảng `majors` là bảng “1”
  * Bảng `students` là bảng “N” và chứa `major_id` làm FK

### 2.2 Tạo bảng majors và Alter bảng students

* FK luôn nằm ở bảng “N” → `students.major_id` trỏ về `majors.id`

```sql
-- db/migration/V1.0.2__create_table_majors.sql

SET search_path TO app;

CREATE TABLE IF NOT EXISTS majors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    major_code VARCHAR(50) NOT NULL UNIQUE,
    major_name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_majors_updated_at ON majors;
CREATE TRIGGER trg_majors_updated_at
BEFORE UPDATE ON majors
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

* Alter bảng `students` để thêm quan hệ với bảng `majors`
  * Bên N (`students`) sẽ chứa FK (`major_id`) tham chiếu sang bên 1 (`majors`)
  * Quan hệ này là `optional` vì có thể có học viên dự thính (chưa phân chuyên ngành)

```sql
-- db/migration/V1.0.3__alter_students_add_major_relation.sql

SET search_path TO app;

ALTER TABLE students
ADD COLUMN IF NOT EXISTS major_id UUID;

ALTER TABLE students
ADD CONSTRAINT fk_students_major
FOREIGN KEY (major_id)
REFERENCES majors(id);
```

Lưu ý: `major_id` là `optional` (có thể NULL) cho học viên dự thính

* Seed dữ liệu mẫu cho chuyên ngành

```sql
-- db/migration/V1.0.4__seed_majors.sql

SET search_path TO app;

INSERT INTO majors (major_code, major_name, description)
VALUES
    ('IT', 'Information Technology', 'Focus on software engineering, networking, databases, AI, and web development'),
    ('SE', 'Software Engineering', 'Specialization in software lifecycle, design patterns, testing and DevOps'),
    ('AI', 'Artificial Intelligence', 'Machine learning, deep learning, computer vision and data science'),
    ('BA', 'Business Administration', 'Management, marketing, accounting and business operations'),
    ('DS', 'Data Science', 'Big Data, statistical modeling, analytics and data mining'),
    ('CE', 'Computer Engineering', 'Hardware, embedded systems, IoT and electronic design')
ON CONFLICT DO NOTHING;
```

* Seed dữ liệu mẫu cho học viên thuộc chuyên ngành

```sql
-- db/migration/V1.0.5__seed_students_with_majors.sql

SET search_path TO app;

-- USERS
INSERT INTO users (username, email, password_hash, status)
VALUES
('truong.tuan', 'tuan.truong@example.com', 'hashed_pw_s1', 'ACTIVE'),
('pham.hoang', 'hoang.pham@example.com', 'hashed_pw_s2', 'ACTIVE'),
('do.trang', 'trang.do@example.com', 'hashed_pw_s3', 'ACTIVE'),
('nguyen.linh', 'linh.nguyen@example.com', 'hashed_pw_s4', 'ACTIVE'),
('le.hai', 'hai.le@example.com', 'hashed_pw_s5', 'ACTIVE'),
('pham.nhi', 'nhi.pham@example.com', 'hashed_pw_s6', 'ACTIVE'),
('bui.dang', 'dang.bui@example.com', 'hashed_pw_s7', 'ACTIVE'),
('ho.thai', 'thai.ho@example.com', 'hashed_pw_s8', 'ACTIVE'),
('ngo.khanh', 'khanh.ngo@example.com', 'hashed_pw_s9', 'ACTIVE'),
('vo.huyen', 'huyen.vo@example.com', 'hashed_pw_s10', 'ACTIVE'),
('quach.nam', 'nam.quach@example.com', 'hashed_pw_s11', 'ACTIVE'),
('tran.dung', 'dung.tran@example.com', 'hashed_pw_s12', 'ACTIVE'),
('pham.quynh', 'quynh.pham@example.com', 'hashed_pw_s13', 'ACTIVE'),
('dao.khoa', 'khoa.dao@example.com', 'hashed_pw_s14', 'ACTIVE'),
('le.vy', 'vy.le@example.com', 'hashed_pw_s15', 'ACTIVE'),
('ngo.minh', 'minh.ngo@example.com', 'hashed_pw_s16', 'ACTIVE'),
('hoang.yen', 'yen.hoang@example.com', 'hashed_pw_s17', 'ACTIVE'),
('phan.son', 'son.phan@example.com', 'hashed_pw_s18', 'ACTIVE'),
('vu.thao', 'thao.vu@example.com', 'hashed_pw_s19', 'ACTIVE'),
('tran.hoa', 'hoa.tran@example.com', 'hashed_pw_s20', 'ACTIVE');

-- PEOPLE (lấy user_id dựa trên email)
INSERT INTO people (full_name, dob, phone, contact_email, address, user_id)
SELECT ps.full_name, ps.dob, ps.phone, ps.contact_email, ps.address, u.id
FROM (
    VALUES
    ('Truong Tuan', DATE '2004-02-14', '0916001001', 'tuan.truong@example.com', 'Hai Chau'),
    ('Pham Hoang', DATE '2005-11-21', '0916001002', 'hoang.pham@example.com', 'Thanh Khe'),
    ('Do Trang', DATE '2003-04-03', '0916001003', 'trang.do@example.com', 'Cam Le'),
    ('Nguyen Linh', DATE '2004-12-10', '0916001004', 'linh.nguyen@example.com', 'Lien Chieu'),
    ('Le Hai', DATE '2006-05-25', '0916001005', 'hai.le@example.com', 'Hai Chau'),
    ('Pham Nhi', DATE '2005-01-18', '0916001006', 'nhi.pham@example.com', 'Hai Chau'),
    ('Bui Dang', DATE '2004-09-09', '0916001007', 'dang.bui@example.com', 'Son Tra'),
    ('Ho Thai', DATE '2003-10-20', '0916001008', 'thai.ho@example.com', 'Hai Chau'),
    ('Ngo Khanh', DATE '2004-07-27', '0916001009', 'khanh.ngo@example.com', 'Thanh Khe'),
    ('Vo Huyen', DATE '2005-06-15', '0916001010', 'huyen.vo@example.com', 'Lien Chieu'),
    ('Quach Nam', DATE '2004-01-07', '0916001011', 'nam.quach@example.com', 'Hai Chau'),
    ('Tran Dung', DATE '2005-03-29', '0916001012', 'dung.tran@example.com', 'Thanh Khe'),
    ('Pham Quynh', DATE '2005-08-11', '0916001013', 'quynh.pham@example.com', 'Cam Le'),
    ('Dao Khoa', DATE '2004-11-01', '0916001014', 'khoa.dao@example.com', 'Lien Chieu'),
    ('Le Vy', DATE '2006-02-23', '0916001015', 'vy.le@example.com', 'Hai Chau'),
    ('Ngo Minh', DATE '2005-09-30', '0916001016', 'minh.ngo@example.com', 'Lien Chieu'),
    ('Hoang Yen', DATE '2004-10-08', '0916001017', 'yen.hoang@example.com', 'Hai Chau'),
    ('Phan Son', DATE '2003-07-26', '0916001018', 'son.phan@example.com', 'Son Tra'),
    ('Vu Thao', DATE '2005-05-12', '0916001019', 'thao.vu@example.com', 'Hai Chau'),
    ('Tran Hoa', DATE '2004-03-19', '0916001020', 'hoa.tran@example.com', 'Cam Le')
) AS ps(full_name, dob, phone, contact_email, address)
JOIN users u ON u.email = ps.contact_email;

-- STUDENTS & gán major_id
INSERT INTO students (person_id, student_code, enrollment_year, major_id)
SELECT
    p.id,
    CONCAT('STU', LPAD((ROW_NUMBER() OVER()) + 7, 3, '0')), -- bắt đầu STU008 → STU027
    FLOOR(RANDOM() * 3) + 2022, -- 2022, 2023 hoặc 2024
    m.id
FROM people p
JOIN users u ON u.id = p.user_id
JOIN majors m ON m.major_code IN (
    'IT', 'SE', 'AI', 'BA', 'DS', 'CE'
)
WHERE u.email LIKE '%@example.com'
  AND u.email NOT IN (
      'an.nguyen@example.com',
      'bich.tran@example.com',
      'cuong.le@example.com',
      'diep.pham@example.com',
      'em.vo@example.com',
      'gia.do@example.com',
      'kien.hoang@example.com'
  )
ORDER BY p.contact_email
LIMIT 20;
```

### 2.3 Many-to-One ở phía Student (bên sở hữu FK)

* Many-to-One: 
  * Ở level DB: Nhiều bản ghi ở bảng `students` liên kết tới 1 bản ghi ở bảng `majors`
  * Ở level entity: Nhiều entity `Student` tham chiếu tới cùng một entity `Major`
* Trong JPA, phía có @JoinColumn là bên sở hữu FK
* Ở quan hệ Major–Student:
  * `Student` giữ `major_id` → `Student` là bên nắm FK
* Chỉ cần mapping một chiều (unidirectional) `@ManyToOne` ở `Student` là đã đủ để JPA hiểu quan hệ
  * `Student` có field `major` là `@JoinColumn` đến entity `Major` → biết `Student` là biết `Major`  
  * Nhưng `Major` không cần biết `Student` 

#### 2.3.1 Tạo entity `Major`

```java
// student/management/api_app/model/Major.java

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "majors", schema = "app")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "major_name", nullable = false, length = FieldLength.NAME_MAX_LENGTH)
    String name;

    @Column(name = "major_code", nullable = false, unique = true, length = FieldLength.STUDENT_CODE_MAX_LENGTH)
    String code;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    Instant updatedAt;
}
```

* `@Column(columnDefinition = "TEXT")`: Trường này trong DB phải là kiểu `TEXT`
  * Mặc định không khai báo `columnDefinition` thì JPA / Hibernate sẽ hiểu `description` trong DB là `VARCHAR` với độ dài giới hạn

#### 2.3.2 Mapping Many-to-One ở Student

```java
// student/management/api_app/model/Student.java

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "students", schema = "app")
public class Student {
    @Id
    @Column(name = "person_id")
    UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id")
    Person person;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_id") // Cột FK trong bảng students
    Major major;

    @Column(name = "student_code", unique = true, nullable = false, length = FieldLength.STUDENT_CODE_MAX_LENGTH)
    String studentCode;

    @Column(name = "enrollment_year")
    Integer enrollmentYear;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    Instant updatedAt;
}
```

* Lưu ý về `CASCADE` trong quan hệ `@ManyToOne`: KHÔNG dùng cascade đến bảng là danh mục chung (category / master data) 
  * Ở đây `Major` là bảng category, nhiều `Student` dùng chung 
  * `Major` là dữ liệu do admin quản lý, không phải do `Student` tạo ra
  * `Student` chỉ tham chiếu đến `Major`, không sở hữu `Major`
  * Chỉ cần JPA / Hibernate lưu `id` của `Major` trong bảng `students`
  * Tác hại tiềm ẩn khi bật cascade:

  | Tác vụ trên `Student` | Ảnh hưởng xấu đến `Major`                  |
  |-----------------------|--------------------------------------------|
  | Lưu `student` mới     | Có thể tạo mới `Major` sai lệch            |
  | Update `student`      | Có thể update `Major` không mong muốn      |
  | Xóa `student`         | Có thể xóa luôn `Major` nếu cascade REMOVE |

### 2.4 Mapping One-to-Many ở phía Major (bidirectional)

> Quan hệ 2 chiều (bidirectional)
> * Nếu chỉ có `@ManyToOne` ở Student → quan hệ 1 chiều: từ `Student` truy ra `Major`
> * Thực tế ta thường muốn từ `Major` truy ngược lại danh sách `Student` → thêm `@OneToMany(mappedBy = "major")`

```java
// student/management/api_app/model/Major.java

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "majors", schema = "app")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Major {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  UUID id;

  @Column(name = "major_name", nullable = false, length = FieldLength.NAME_MAX_LENGTH)
  String name;

  @Column(name = "major_code", nullable = false, unique = true, length = FieldLength.STUDENT_CODE_MAX_LENGTH)
  String code;

  @Column(columnDefinition = "TEXT")
  String description;

  @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
  Instant createdAt;

  @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
  Instant updatedAt;

  @OneToMany(
          mappedBy = "major", // tên field ở Student
          fetch = FetchType.LAZY)
  @Builder.Default
  List<Student> students = new ArrayList<>();
}
```

* `mappedBy = "major"`:
  * "major" phải trùng với tên field private `Major major` trong entity `Student`
  * JPA sẽ hiểu FK nằm ở bên `Student`, không phải bên `Major`
  * Vì vậy JPA không thêm cột FK nào vào `Major`, chỉ đơn thuần là view logic
* Không dùng `@JoinColum` trên field `students` vì ta không mong muốn JPA tạo cột FK ở bảng `majors` 
* Lưu ý về `CASCADE` trong quan hệ `@OneToMany`: chỉ nên bật cascade ở phía cha khi dữ liệu con thuộc sở hữu trọn vẹn của cha (cha: `Blog` → con: `Comments`)
  * Ở đây `Student` thuộc về `Major`, nhưng `Major` không sở hữu `Student`
    * `Major` là bảng master / danh mục ngành → dữ liệu do Admin quản lý
    * `Student` là dữ liệu nghiệp vụ → nhiều `student` tham chiếu đến 1 `major`
  * `Major` không phải cha của `Student` → không bật cascade

### 2.5 Thao tác trong `Service`

* Lấy danh sách `Student` theo `Major`: nhờ có `@OneToMany(...) List<Student> students` ở `Major` mà ta có thể:

```java
Major major = majorRepo.findById(id).get();
List<Student> list = major.getStudents();
```

Khi đó Hibernate sẽ thực hiện truy vấn:

```sql
-- majorRepo.findById(id).get()
SELECT * FROM app.majors WHERE id = ?

-- major.getStudents()
SELECT s.* FROM app.students s WHERE s.major_id = ?
```

* Gán `Major` cho `Student` khi tạo mới `Student`:

```java
public record StudentCreateOnlyRequest(
        String studentCode,
        Integer enrollmentYear,
        UUID majorId // thêm field majorId cho request DTO
) {
}
```

```java
@Transactional
public StudentDetailResponse create(StudentCreateRequest req) {
    // ... phần tạo Person

    // Thao tác với Major
    UUID majorId = req.student().majorId();

    Major major = null;
    if (majorId != null) {
        major = majorRepo.findById(majorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Major not found: " + majorId));
    }

    // Tạo student mới
    Student student = Student.builder()
            .person(p)
            .studentCode(studentCode)
            .enrollmentYear(sReq.enrollmentYear())
            .major(major) // gán tham chiếu đối tượng major với student  
            .build();

    // save student ...
}
```

**Lưu ý**: luôn gán ở bên sở hữu FK (`student.setMajor(major)`) thì JPA mới cập nhật `major_id` cho bảng `students`

---

## 3) Many-to-Many Mapping

### 3.1 Nghiệp vụ Student - Course

* Một `Student` có thể đăng ký nhiều `Course`
* Một `Course` có nhiều `Student` tham gia
* Và mỗi lần đăng ký cần có thêm thông tin:
  * `enrolledAt`: ngày đăng ký / ngày bắt đầu học
  * `status`: đang học, đã hoàn thành, hủy
  * `grade`: điểm cuối kỳ

Quan hệ `N–N` trong thực tế thường được triển khai bằng một thực thể trung gian `Enrollment` (đăng ký học) → quan hệ thực tế là `Student 1 --- N Enrollment N --- 1 Course`

> Hễ quan hệ `N–N` mà có thêm field nghiệp vụ (`grade`, `status`, `enrolledAt`, ...) →
Phải tách thành 1 entity trung gian, không dùng `@ManyToMany` trực tiếp

### 3.2 Tạo bảng cho Course và Enrollment

* Tạo bảng `courses`:

```sql
-- db/migration/V1.0.6__create_table_courses.sql

SET search_path TO app;

CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_code VARCHAR(50) NOT NULL UNIQUE,
    course_name VARCHAR(150) NOT NULL,
    description TEXT,
    credit INT NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_courses_updated_at ON courses;
CREATE TRIGGER trg_courses_updated_at
BEFORE UPDATE ON courses
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

* Tạo bảng `enrollments`

```sql
-- db/migration/V1.0.7__create_table_enrollments.sql

SET search_path TO app;

CREATE TABLE IF NOT EXISTS enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    student_id UUID NOT NULL,
    course_id UUID NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'ENROLLED', -- ENROLLED / COMPLETED / DROPPED
    grade VARCHAR(10),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_id) REFERENCES students(person_id),
    CONSTRAINT fk_enrollments_course
        FOREIGN KEY (course_id) REFERENCES courses(id),

    -- Business rule: 1 student - 1 course chỉ có 1 lần đăng ký (enrollment)
    CONSTRAINT uk_enrollments_student_course
        UNIQUE (student_id, course_id)
);

DROP TRIGGER IF EXISTS trg_enrollments_updated_at ON enrollments;
CREATE TRIGGER trg_enrollments_updated_at
BEFORE UPDATE ON enrollments
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
```

### 3.3 Entity mapping với @OneToMany + @ManyToOne

#### 3.3.1 Entity Course

```java
// student/management/api_app/model/Course.java

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "courses", schema = "app")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;
  
    @Column(name = "course_name", nullable = false, length = FieldLength.NAME_MAX_LENGTH)
    String name;
  
    @Column(name = "course_code", nullable = false, unique = true, length = FieldLength.CODE_MAX_LENGTH)
    String code;
  
    @Column(columnDefinition = "TEXT")
    String description;
  
    @Column(nullable = false)
    Integer credit;
  
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    Instant createdAt;
  
    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    Instant updatedAt;
  
    @OneToMany(mappedBy = "course", fetch = FetchType.LAZY)
    @Builder.Default
    List<Enrollment> enrollments = new ArrayList<>();
}
```

#### 3.3.2 Entity Enrollment (thực thể trung gian)

* Ở DB: `enrollments` chứa 2 FK `student_id`, `course_id`
* Entity `Enrollment` có 2 quan hệ Many-to-One:
  * `Enrollment` → `Student`
  * `Enrollment` → `Course`

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "enrollments", schema = "app",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollments_student_course",
                columnNames = {"student_id", "course_id"}))
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    Course course;

    @Column(nullable = false, length = FieldLength.STATUS_MAX_LENGTH)
    String status;

    @Column(length = FieldLength.GRADE_MAX_LENGTH)
    String grade;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    Instant updatedAt;
}
```

### 3.4 Thao tác đăng ký học trong Service

Đăng ký `student` vào một `course`:

```java
@Transactional
public EnrollmentResponse enrollStudentToCourse(UUID studentId, UUID courseId) {
    Student student = studentRepo.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Student not found: " + studentId));

    Course course = courseRepo.findById(courseId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Course not found: " + courseId));

    // Kiểm tra ràng buộc UNIQUE (student_id, course_id) <=> enrollment tồn tại chưa
    if (enrollmentRepo.existsByStudentAndCourse(student, course)) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Student already enrolled in this course");
    }

    Enrollment enrollment = Enrollment.builder()
            .student(student)
            .course(course)
            .enrolledAt(Instant.now())
            .status("ENROLLED")
            .build();

    enrollmentRepo.save(enrollment);

    return enrollmentMapper.toResponse(enrollment);
}
```

* Thao tác chủ yếu trên entity trung gian `Enrollment`
* Việc thêm / xóa quan hệ Many-to-Many giờ trở thành:
  * Thêm / xóa một bản ghi `Enrollment`
* Dễ kiểm soát business rule (ràng buộc unique, ...)

### 3.5 Entity mapping với @ManyToMany

JPA vẫn hỗ trợ @ManyToMany, tuy nhiên chỉ nên sử dụng cho các quan hệ rất đơn giản, ví dụ:
* Một User thuộc nhiều Role, mỗi Role gán cho nhiều User
* Không có thêm field nghiệp vụ nào ở giữa

_Note: sẽ được học cụ thể ở bài Phân quyền_

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "users_roles", 
        schema = "app",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
)
Set<Role> roles = new HashSet<>();
```

---

## 4) Join operations, N+1 problem & cách xử lý

### 4.1 Join trong JPA – bản chất & vấn đề hiệu năng

Trong JPA/Hibernate:
* Mỗi entity mapping có thể được fetch (truy xuất) theo 2 cách:
  * `EAGER`: Load quan hệ ngay lập tức (kể cả không dùng tới)
  * `LAZY`: Load quan hệ khi gọi tới getter (lúc thật sự cần)
* Khi `LAZY`, Hibernate sẽ tạo query riêng để lấy dữ liệu liên quan khi nó được truy cập

→ Nếu truy cập nhiều phần tử trong danh sách, có thể dẫn đến N+1 Query Problem

### 4.2 N+1 problem đã gặp

API `GET /students` trả về danh sách student trong Page:

* Nếu client truyền `size=2` → hibernate sẽ lặp 2 câu SQL y hệt để load dữ liệu `Person` tương ứng từng `Student` khi mapper truy cập field `Student.person`
* Query bị lặp: `SELECT * FROM app.people WHERE id = :person_id;`

```java
// StudentService

Page<Student> pageData = studentRepo.findAll(pageable);

Page<StudentListItemResponse> mappedPageData =
        pageData.map(studentMapper::toListItemResponse);

// Mapper
public StudentListItemResponse toListItemResponse(Student s) {
  Person p = s.getPerson();   // chỗ gây N+1

  return new StudentListItemResponse(
          s.getId(),
          s.getStudentCode(),
          s.getEnrollmentYear(),
          p.getFullName(),
          p.getContactEmail(),
          AgeCalculator.isAdult(p.getDob())
  );
}
```

* `studentRepo.findAll(pageable)` → Hibernate chạy 1 query lấy danh sách students (không join people)
* `Person p = s.getPerson()` → với mỗi `student` Hibernate sẽ chạy 1 query riêng `SELECT * FROM app.people WHERE id = :person_id;`
* Có 2 `student` trong page:
  * 1 query lấy students
  * 2 query lấy person tương ứng
* Gây ra N+1 query, chính xác là: `1 (students) + N (people)`

→ Nếu Page có 1000 `student`, N+1 queries gây chậm đáng kể

### 4.3 Giải pháp xử lý N+1

#### 4.3.1 Dùng @EntityGraph

Trong entity `Student` đang dùng `FetchType.LAZY`: 

```java
@MapsId
@OneToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "person_id")
Person person;
```

Khi `StudentService` chạy:

```java
public PageResponse<StudentListItemResponse> getAll(Pageable pageable) {
    Page<Student> students = studentRepo.findAll(pageable);
    students.forEach(s -> s.getPerson());
    // return ...
}
```

Hibernate thực thi tổng cộng N+1 query:
1. 1 query lấy danh sách Student
2. N query tiếp theo để load person cho từng student

→ Dùng `@EntityGraph` thay đổi cách load dữ liệu:

```java
// StudentRepository

@Override
@NonNull // Dùng springframework.lang.@NonNull để loại cảnh báo "method annotated with @NonNullApi"
@EntityGraph(attributePaths = "person")
Page<Student> findAll(@NonNull Pageable pageable); 
```

Nôm na dễ hiểu là đang ra lệnh cho Hibernate:
> Khi load `Student` qua method `findAll()` này, hãy load luôn thuộc tính `person` trong cùng 1 query, dù nó là `LAZY` trong entity

→ Kết quả: Hibernate ghi đè `fetch type` tạm thời cho thuộc tính `person` (chỉ trong query này) và sinh 1 query JOIN:

```sql
SELECT s.*, p.*
FROM students s
LEFT JOIN people p ON p.id = s.person_id;
```

Thực chất `@EntityGraph` tạo JOIN FETCH để load luôn entity `Person` vào `student.person`

#### 4.3.2 Dùng @Query JPQL với DTO thay vì trả entity `Student`

Đây là cách hiệu quả nhất khi trả về danh sách với các field theo DTO chứ không trả toàn bộ field của entity:

```java
// StudentRepository
@Query("""
    SELECT new student.management.api_app.dto.student.StudentListItemResponse(
        s.id,
        s.studentCode,
        s.enrollmentYear,
        p.fullName,
        p.contactEmail,
        CASE WHEN p.dob <= :eighteenYearsAgo THEN true ELSE false END
    )
    FROM Student s
    LEFT JOIN s.person p
    """)
Page<StudentListItemResponse> findAllListItem(
        @Param("eighteenYearsAgo") LocalDate eighteenYearsAgo,
        Pageable pageable);

// StudentService
LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);

Page<StudentListItemResponse> pageData =
        studentRepo.findAllListItem(eighteenYearsAgo, pageable);
```

#### 4.3.3 Batch size khi buộc phải lazy load nhiều phần tử

Nếu bắt buộc phải lazy load nhiều query, có thể giảm số lượng query bằng cách bật `batch fetch`:

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=50
```

Khi đó thay vì 1000 query để lấy 1000 `person`, Hibernate gom thành:
* 1 query để lấy `student`
* 20 query để lấy `person` theo batch (50 bản ghi / lần)

---

## 5) Thực hành

### 5.1 Xử lý N+1 cho API `GET /students`

#### 5.1.1 Dùng @EntityGraph

* Chỉ cần điều chỉnh `findAll()` ở `StudentRepository`:

```java
// student/management/api_app/repository/StudentRepository.java
@Repository
public interface StudentRepository
        extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

    @Override
    @NonNull
    @EntityGraph(attributePaths = "person")
    Page<Student> findAll(@NonNull Pageable pageable);

    // Các method khác ...
}
```

* Kiểm tra kết quả log SQL

```sql
Hibernate: 
    select
        s1_0.person_id,
        s1_0.created_at,
        s1_0.enrollment_year,
        s1_0.major_id,
        p1_0.id,
        p1_0.address,
        p1_0.contact_email,
        p1_0.created_at,
        p1_0.dob,
        p1_0.full_name,
        p1_0.phone,
        p1_0.updated_at,
        s1_0.student_code,
        s1_0.updated_at 
    from
        app.students s1_0 
    join
        app.people p1_0 
            on p1_0.id=s1_0.person_id 
    offset
        ? rows 
    fetch
        first ? rows only
```

#### 5.1.2 Dùng @Query JPQL với StudentListItemResponse DTO

* Thêm method `findAllListItem()` ở `StudentRepository`:

```java
@Repository
public interface StudentRepository
        extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

    @Query("""
        SELECT new student.management.api_app.dto.student.StudentListItemResponse(
            s.id,
            s.studentCode,
            s.enrollmentYear,
            p.fullName,
            p.contactEmail,
            CASE WHEN p.dob <= :eighteenYearsAgo THEN true ELSE false END
        )
        FROM Student s
        LEFT JOIN s.person p
    """)
    Page<StudentListItemResponse> findAllListItem(
            @Param("eighteenYearsAgo") LocalDate eighteenYearsAgo,
            Pageable pageable);
}
```

* Ở `StudentService` điều chỉnh `getAll()`: 

```java
// student/management/api_app/service/impl/StudentService.java
@Service
@RequiredArgsConstructor
public class StudentService implements IStudentService {

    @Transactional(readOnly = true)
    @Override
    public PageResponse<StudentListItemResponse> getAll(Pageable pageable) {
        Page<StudentListItemResponse> pageData =
                studentRepo.findAllListItem(AgeCalculator.eighteenYearsAgo(), pageable);
        return new PageResponse<>(pageData);
    }
}

// student/management/api_app/util/AgeCalculator.java
public class AgeCalculator {
    public static LocalDate eighteenYearsAgo() {
        return LocalDate.now().minusYears(18);
    }
}
```

* Kiểm tra kết quả log SQL:

```sql
Hibernate: 
    select
        s1_0.person_id,
        s1_0.student_code,
        s1_0.enrollment_year,
        p1_0.full_name,
        p1_0.contact_email,
        case 
            when p1_0.dob<=? 
                then true 
            else false 
    end 
from
    app.students s1_0 
join
    app.people p1_0 
        on p1_0.id=s1_0.person_id 
fetch
    first ? rows only
```

#### Bài tập 1: Hãy kiểm tra tất cả API còn lại có dính N+1 problem & refactor để xử lý N+1

### 5.2 Cập nhật DTO Response của Student để trả thêm thông tin của Major

Trong các DTO Response của Student cần thêm các field liên quan `Student.major`:

#### 5.2.1 StudentDetailResponse

* Tạo `MajorDetailResponse`:

```java
// student/management/api_app/dto/major/MajorDetailResponse.java
public record MajorDetailResponse(
        UUID id,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
```

* Tạo `MajorListItemResponse`:

```java
// student/management/api_app/dto/major/MajorListItemResponse.java
public record MajorListItemResponse(
        UUID id,
        String code,
        String name
) {
}
```

* Thêm object `majorDetail` khi trả về chi tiết `student`:

```java
// student/management/api_app/dto/student/StudentDetailResponse.java
public record StudentDetailResponse(
        PersonDetailResponse personDetail,
        MajorDetailResponse majorDetail,

        String studentCode,
        Integer enrollmentYear,
        Instant createdAt,
        Instant updatedAt
) {
}
```

* Tạo `MajorMapper`:

```java
// student/management/api_app/mapper/MajorMapper.java

@Component
public class MajorMapper {

  public MajorDetailResponse toDetailResponse(Major major) {
    return major == null ? null : new MajorDetailResponse(
            major.getId(),
            major.getCode(),
            major.getName(),
            major.getDescription(),
            major.getCreatedAt(),
            major.getUpdatedAt()
    );
  }

  public MajorListItemResponse toListItemResponse(Major major) {
    return new MajorListItemResponse(
            major.getId(),
            major.getCode(),
            major.getName()
    );
  }
}
```

* Cập nhật method `StudentMapper.toDetailResponse()` để trả thêm object `student.getMajor()`:

```java
// student/management/api_app/mapper/StudentMapper.java

private final MajorMapper majorMapper;

public StudentDetailResponse toDetailResponse(Student s) {
  return new StudentDetailResponse(
          personMapper.toDetailResponse(s.getPerson()),
          majorMapper.toDetailResponse(s.getMajor()),

          s.getStudentCode(),
          s.getEnrollmentYear(),
          s.getCreatedAt(),
          s.getUpdatedAt()
  );
}
```

#### 5.2.2 StudentListItemResponse

* Thêm `majorCode` khi trả về list item `student`:

```java
// student/management/api_app/dto/student/StudentListItemResponse.java
public record StudentDetailResponse(
        PersonDetailResponse personDetail,
        MajorDetailResponse majorDetail,

        String studentCode,
        Integer enrollmentYear,
        Instant createdAt,
        Instant updatedAt
) {
}
```

* Cập nhật method `StudentMapper.toListItemResponse()` để trả thêm `major.getCode()`:

```java
// student/management/api_app/mapper/StudentMapper.java
public StudentListItemResponse toListItemResponse(Student s) {
  Person p = s.getPerson();
  Major m = s.getMajor(); // Có thể null vì Student.major là @ManyToOne(optional=true)
  String majorCode = Optional.ofNullable(m)
          .map(Major::getCode)
          .orElse(null);

  return new StudentListItemResponse(
          s.getId(),
          s.getStudentCode(),
          s.getEnrollmentYear(),
          p.getFullName(),
          p.getContactEmail(),
          AgeCalculator.isAdult(p.getDob()),
          majorCode
  );
}
```

#### Lưu ý N+1 problem:

> Cần kiểm tra N+1 problem đối với tất cả những API trả về `StudentListItemResponse` và xử lý
> * `StudentRepository.findAllListItem()`
> * `StudentRepository.findAll()`
> * `StudentRepository.findByEnrollmentYear()`

```java
// student/management/api_app/repository/StudentRepository.java
@Query("""
SELECT new student.management.api_app.dto.student.StudentListItemResponse(
    s.id,
    s.studentCode,
    s.enrollmentYear,
    p.fullName,
    p.contactEmail,
    CASE WHEN p.dob <= :eighteenYearsAgo THEN true ELSE false END,
    m.code
)
FROM Student s
LEFT JOIN s.person p
LEFT JOIN s.major m
""")
Page<StudentListItemResponse> findAllListItem(
        @Param("eighteenYearsAgo") LocalDate eighteenYearsAgo,
        Pageable pageable);

@Override
@NonNull
@EntityGraph(attributePaths = {"person", "major"})
Page<Student> findAll(Specification spec, @NonNull Pageable pageable);

@EntityGraph(attributePaths = {"person", "major"})
Page<Student> findByEnrollmentYear(Integer enrollmentYear, Pageable pageable);
```

### 5.3 Thêm filter majorCode cho API GET students/search

#### 5.3.1 Thêm nested MajorSearchRequest trong StudentSearchRequest

```java
// student/management/api_app/dto/student/StudentSearchRequest.java
public record StudentSearchRequest(

        PersonSearchRequest person,
        MajorSearchRequest major,

        String studentCode,
        Integer enrollmentYearFrom,
        Integer enrollmentYearTo
) {}
``` 

#### 5.3.2 Thêm các Specification con trong StudentSpecifications

```java
// student/management/api_app/repository/specification/StudentSpecifications.java
public static Specification<Student> majorCodeContains(String code) {
  return (root, query, cb) -> {
    if (!StringUtils.hasText(code)) return null;
    // LEFT JOIN luôn giữ lại Student, kể cả khi major = null
    Join<Student, Major> major = root.join("major", JoinType.LEFT);
    return cb.like(cb.lower(major.get("code")), SpecUtils.likePattern(code));
  };
}

public static Specification<Student> majorNameContains(String name) {
  return (root, query, cb) -> {
    if (!StringUtils.hasText(name)) return null;
    Join<Student, Major> major = root.join("major", JoinType.LEFT);
    return cb.like(cb.lower(root.get("name")), SpecUtils.likePattern(name));
  };
}
```

#### 5.3.3 Gắn filter major vào StudentService.search(...)

```java
// student/management/api_app/service/impl/StudentService.java
String majorCode = normalizeCode(mReq != null ? mReq.code() : null);
String majorName = trimToNull(mReq != null ? mReq.name() : null);

Specification<Student> spec = Specification.<Student>unrestricted()
        .and(personNameContains(name))
        .and(personPhoneEquals(phone))
        .and(personEmailContains(email))
        .and(personDobGte(pReq != null ? pReq.dobFrom() : null))
        .and(personDobLte(pReq != null ? pReq.dobTo() : null))
        .and(studentCodeContains(studentCode))
        .and(enrollmentYearGte(req.enrollmentYearFrom()))
        .and(enrollmentYearLte(req.enrollmentYearTo()))
        .and(majorCodeContains(majorCode))
        .and(majorNameContains(majorName));
```

### 5.4 Thêm API `GET /by-major/{majorId}`

#### 5.4.1 StudentRepository

```java
// findByMajor_Id = truy vấn nested theo student.major.id
Page<Student> findByMajor_Id(UUID id, Pageable pageable);
```

#### 5.4.2 StudentService

```java
@Transactional(readOnly = true)
@Override
public PageResponse<StudentListItemResponse> listByMajorId(
        UUID majorId, Pageable pageable) {

    if (!majorRepo.existsById(majorId)) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Major not found with id: " + majorId);
    }

    Page<StudentListItemResponse> pageData =
            studentRepo.findByMajor_Id(majorId, pageable)
                    .map(studentMapper::toListItemResponse);

    return new PageResponse<>(pageData);
}
```

#### 5.4.3 StudentController

```java
@Operation(
        summary = "List students by major id with pagination",
        description = "Lấy danh sách student theo id của chuyên ngành có phân trang",
        responses = {
                @ApiResponse(responseCode = "200", description = "Success"),
                @ApiResponse(responseCode = "200", description = "Success",
                        content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
        }
)
@GetMapping("/by-major/{major_id}")
public ResponseEntity<AppResponse<PageResponse<StudentListItemResponse>>> listStudentsByMajor(
        @PathVariable UUID major_id,
        @ParameterObject
        @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    return ResponseEntity.ok(AppResponse.success(service.listByMajorId(major_id, pageable)));
}
```

#### Bài tập 2: Hãy viết các API cho entity Major

1. `GET /majors`: Lấy danh sách tất cả học viên có phân trang và tìm kiếm theo các thuộc tính
2. `GET /majors/{id}`: Lấy chi tiết chuyên ngành theo ID
3. `GET /majors/by-major-code`: Lấy chi tiết chuyên ngành theo major code
4. `POST /majors/bulk-by-ids`: Lấy nhiều chuyên ngành theo list id (nhận danh sách UUID qua body (POST) để tránh giới hạn độ dài URL)
5. `POST /majors`: Tạo chuyên ngành mới. Trả về 201 Created và Location header
6. `PUT /majors/{id}`: Cập nhật cho major theo ID (cập nhật toàn bộ, không phải PATCH)
7. `DELETE /majors/{id}`: Xóa chuyên ngành theo ID
