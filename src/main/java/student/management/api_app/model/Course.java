package student.management.api_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import student.management.api_app.constant.FieldLength;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
