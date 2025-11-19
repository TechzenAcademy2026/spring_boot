# Spring Boot – Buổi 9: Criteria API / QueryDSL & MapStruct

## 1) Criteria API (chuẩn Java EE)

### 1.1 Tổng quan

> Criteria API là API trong JPA cho phép xây dựng câu truy vấn bằng **Java code** thay vì chuỗi JPQL <br>
> Thay vì `SELECT s FROM Student s WHERE ...` → ta build từng phần bằng `CriteriaBuilder`, `CriteriaQuery`, `Root`, `Predicate`

| Thành phần            | Vai trò                                                              |
|-----------------------|----------------------------------------------------------------------|
| `CriteriaBuilder`     | Factory tạo các biểu thức, `Predicate`, `CriteriaQuery`              |
| `CriteriaQuery`       | Đại diện cho toàn bộ câu query                                       |
| `Root<T>`             | Gốc của query, đại diện cho entity chính (ví dụ `Student`)           |
| `Predicate`           | Điều kiện trong `WHERE`, có thể AND/OR/NOT                           |
| `Path` / `Expression` | Đại diện cho cột hoặc biểu thức (`fullName`, `LOWER(fullName)`, ...) |

* Trong Specification, method `toPredicate(root, query, cb)` chính là chỗ sử dụng Criteria API 

```java
public interface Specification<T> {
    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);
}
``` 

* Thực chất, bên dưới `Specification`, Sping Data JPA tự gọi `toPredicate()` và dùng Criteria API để tự động tạo object `Predicate` biểu diễn điều kiện cho WHERE (ví dụ `LOWER(studentCode) LIKE '%abc%'`)

* Trong Buổi 7 ta đã viết `StudentSpecifications`:

```java
public class StudentSpecifications {
    
    public static Specification<Student> studentCodeContains(String code) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(code)) return null;
            return cb.like(cb.lower(root.get("studentCode")), "%" + code.toLowerCase() + "%");
        };
    }

    public static Specification<Student> enrollmentYearGte(Integer year) {
        return (root, query, cb) -> {
            if (year == null) return null;
            return cb.greaterThanOrEqualTo(root.get("enrollmentYear"), year);
        };
    }
}
```

* Nghĩa là chúng ta đã truyền biểu thức lambda này để implement phương thức abstract `Predicate toPredicate(...)`: 

```
(root, query, cb) -> {
    if (!StringUtils.hasText(code)) return null;
    return cb.like(cb.lower(root.get("studentCode")), "%" + code.toLowerCase() + "%");
};
```

→ Để hiểu rõ Sprig Data JPA sử dụng Criteria API như thế nào, hãy thử build một repository custom dùng Criteria API trực tiếp

### 1.2 Tìm Student bằng Criteria API thuần (không dùng Specification)

Tạo repository custom StudentCriteriaRepository không `extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student>`








```java
@Repository
@RequiredArgsConstructor
public class StudentCriteriaRepository {

    private final EntityManager em;

    public List<Student> search(
            String nameKeyword,
            Integer minYear,
            Integer maxYear,
            String majorCode
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Student> cq = cb.createQuery(Student.class);

        Root<Student> root = cq.from(Student.class);
        // JOIN person & major giống mapping ở buổi 8
        Join<Student, Person> personJoin = root.join("person", JoinType.INNER);
        Join<Student, Major> majorJoin = root.join("major", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        if (nameKeyword != null && !nameKeyword.isBlank()) {
            predicates.add(
                    cb.like(
                            cb.lower(personJoin.get("fullName")),
                            "%" + nameKeyword.toLowerCase() + "%"
                    )
            );
        }

        if (minYear != null) {
            predicates.add(
                    cb.greaterThanOrEqualTo(root.get("enrollmentYear"), minYear)
            );
        }

        if (maxYear != null) {
            predicates.add(
                    cb.lessThanOrEqualTo(root.get("enrollmentYear"), maxYear)
            );
        }

        if (majorCode != null && !majorCode.isBlank()) {
            predicates.add(
                    cb.equal(majorJoin.get("code"), majorCode)
            );
        }

        cq.where(predicates.toArray(Predicate[]::new));
        cq.orderBy(cb.desc(root.get("enrollmentYear")));

        return em.createQuery(cq).getResultList();
    }
}
```

### 📌 Ưu & nhược điểm

| Ưu điểm                                                 | Nhược điểm                     |
| ------------------------------------------------------- | ------------------------------ |
| An toàn khi compile-time (tránh lỗi sai chính tả field) | Cú pháp dài, khó đọc           |
| Tạo truy vấn động mạnh mẽ                               | Khó bảo trì nếu logic phức tạp |
| Áp dụng khi search/filter nhiều trường                  | Không trực quan cho dev mới    |

➡ **Criteria API phù hợp khi cần filter đa điều kiện** (tìm kiếm nâng cao).

---

## 2) QueryDSL – truy vấn mạnh mẽ và hiện đại

### 📌 Tổng quan

QueryDSL là thư viện query type-safe, dễ đọc hơn Criteria API.
→ Cung cấp class Q-Entity để truy vấn an toàn & ngắn gọn.

### 📌 Ví dụ

```java
QStudent s = QStudent.student;

List<Student> students = queryFactory
    .selectFrom(s)
    .where(s.age.gt(18).and(s.fullName.containsIgnoreCase(keyword)))
    .orderBy(s.createdAt.desc())
    .fetch();
```

### 📌 Ưu điểm vượt trội

| Criteria API  | QueryDSL                                    |
| ------------- | ------------------------------------------- |
| Dài, phức tạp | Ngắn gọn, dễ đọc                            |
| Khó debug     | Log rõ ràng                                 |
| Học lâu       | Dễ tiếp cận và phổ biến trong dự án thực tế |

➡ **Nhiều công ty sử dụng QueryDSL thay thế Criteria API cho search/pagination phức tạp.**

---

## **3) MapStruct – Entity ↔ DTO Mapping chuẩn & hiệu năng cao**

### 📌 Vấn đề cần giải quyết

Mapper thủ công tạo nhiều code thừa:

```java
StudentResponse r = new StudentResponse();
r.setFullName(e.getFullName());
...
```

→ Dễ lỗi, tốn thời gian, khó bảo trì.

### 📌 MapStruct – giải pháp tối ưu

```java
@Mapper(componentModel = "spring")
public interface StudentMapper {
    StudentResponse toDto(Student entity);
    Student toEntity(StudentCreateRequest req);
}
```

### 📌 Tạo mapper nâng cao tùy chỉnh

```java
@Mapper(componentModel = "spring")
public interface StudentMapper {
    @Mapping(target = "adult", expression = "java(entity.isAdult())")
    StudentResponse toDto(Student entity);
}
```

### 📌 Cấu hình MapStruct trong Gradle

```gradle
dependencies {
    implementation 'org.mapstruct:mapstruct:1.6.0'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.0'
}
```

---

## **4) Entity DTO Mapping Patterns – Best Practices**

| Kiểu mapping                  | Ưu điểm                  | Nhược                                 |
| ----------------------------- | ------------------------ | ------------------------------------- |
| Entity → frontend trực tiếp   | Nhanh                    | Vi phạm bảo mật, lộ field, nhiều data |
| Entity → DTO mapping bằng tay | Kiểm soát tốt            | Code dài                              |
| **MapStruct (đề xuất)**       | Tối ưu, clean, hiệu năng | Cần setup ban đầu                     |

➡ Trong hệ thống thực tế: **Service → Mapper → DTO → Controller response**.

---

## **5) Response Optimization – tối ưu payload & hiệu năng**

### **Các chiến lược tối ưu response**

Một sai lầm phổ biến là trả toàn bộ Entity ra ngoài API → nặng & lộ dữ liệu

| Chiến lược              | Ví dụ                                 | Khi dùng                         |
| ----------------------- | ------------------------------------- | -------------------------------- |
| DTO projection          | StudentSummaryDTO                     | Hầu hết API real-world           |
| Select fields cần thiết | `SELECT new StudentDTO(s.id, s.name)` | Trả dữ liệu lớn / bảng nhiều cột |
| Pagination              | Page<StudentDTO>                      | Danh sách dài                    |
| Fetch join              | `JOIN FETCH classroom`                | Tránh N+1                        |
| JSON view               | Chia cấp hiển thị field               | Nếu API nhiều vai trò người dùng |


### 📌 Trả DTO thay vì Entity

Tránh lazy-loading ngoài ý muốn & giảm kích thước response.

### 📌 Projection chỉ lấy field cần thiết

```java
public interface StudentListProjection {
    UUID getId();
    String getFullName();
    Integer getAge();
}

List<StudentListProjection> findAllProjectedBy();
```

### 📌 Kết hợp QueryDSL + Pagination + Projection

```java
QStudent s = QStudent.student;

return queryFactory
    .select(Projections.constructor(StudentListResponse.class,
        s.id, s.fullName, s.age))
    .from(s)
    .where(s.isActive.eq(true))
    .offset(page * size)
    .limit(size)
    .fetch();
```

➡ Truy vấn chỉ lấy dữ liệu cần thiết → response nhanh, tiết kiệm RAM.

---

## **6) Thực hành buổi 9**

1️⃣ Cài QueryDSL & MapStruct
2️⃣ Tạo API tìm kiếm Student nâng cao:

* Lọc theo name / age / classroom
* Pagination + Sorting
* Kết hợp DTO Projection để tối ưu response

3️⃣ Viết mapper bằng MapStruct cho:

* `StudentCreateRequest → Student`
* `Student → StudentResponse`
* `Student → StudentListResponse (lite)`

---

## **7) Tổng kết kiến thức**

| Chủ đề                | Ý nghĩa                                           |
| --------------------- | ------------------------------------------------- |
| Criteria API          | Hỗ trợ query động theo chuẩn Java EE              |
| QueryDSL              | Tối ưu cho dự án thực tế, type-safe và clean code |
| MapStruct             | Mapping Entity ↔ DTO hiệu năng cao                |
| Response Optimization | Tối ưu API về hiệu suất và kích thước dữ liệu     |

---

Buổi 10 đề xuất tiếp theo: **JPA Transaction Management & Concurrency Control** (Optimistic / Pessimistic Lock, Retry Strategies).
