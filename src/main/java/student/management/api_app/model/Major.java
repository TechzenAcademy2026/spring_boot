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
@Table(name = "majors", schema = "app")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "major_code", nullable = false, unique = true, length = FieldLength.CODE_MAX_LENGTH)
    String code;

    @Column(name = "major_name", nullable = false, length = FieldLength.NAME_MAX_LENGTH)
    String name;

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
