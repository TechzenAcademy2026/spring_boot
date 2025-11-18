package student.management.api_app.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import student.management.api_app.constant.FieldLength;

import java.time.Instant;
import java.util.UUID;

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
