package student.management.api_app.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import student.management.api_app.dto.student.EnrollmentStatDTO;
import student.management.api_app.dto.student.StudentListItemResponse;
import student.management.api_app.model.Student;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

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

    // findByMajor_Id = truy vấn nested theo student.major.id
    Page<Student> findByMajor_Id(UUID id, Pageable pageable);

    Optional<Student> findByStudentCode(String studentCode);

    boolean existsByStudentCode(String studentCode);

    @Query("""
        SELECT s FROM Student s
        JOIN s.person p
        WHERE p.phone = :phone
    """)
    Optional<Student> findByPhone(@Param("phone") String phone);

    @Query("""
        SELECT new student.management.api_app.dto.student.EnrollmentStatDTO(
            s.enrollmentYear,
            COUNT(s)
        )
        FROM Student s
        GROUP BY s.enrollmentYear
    """)
    Page<EnrollmentStatDTO> countStudentsGroupedByYear(Pageable pageable);
}
