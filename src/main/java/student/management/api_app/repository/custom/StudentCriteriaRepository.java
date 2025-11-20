package student.management.api_app.repository.custom;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import student.management.api_app.model.Major;
import student.management.api_app.model.Person;
import student.management.api_app.model.Student;

import java.util.ArrayList;
import java.util.List;

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
        Join<Student, Person> personJoin = root.join("person", JoinType.INNER);
        Join<Student, Major> majorJoin = root.join("major", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        // Thêm các điều kiện lọc, tương tự như trong StudentSpecifications
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
