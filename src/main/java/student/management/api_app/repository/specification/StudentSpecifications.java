package student.management.api_app.repository.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import student.management.api_app.model.Major;
import student.management.api_app.model.Person;
import student.management.api_app.model.Student;

import java.time.LocalDate;

public class StudentSpecifications {

    public static Specification<Student> personNameContains(String keyword) {
        return (root, query, cb) -> {
            Join<Student, Person> personJoin = root.join("person", JoinType.INNER);
            return PersonSpecifications.fullNameContains(personJoin, cb, keyword);
        };
    }

    public static Specification<Student> personPhoneEquals(String phone) {
        return (root, query, cb) -> {
            Join<Student, Person> person = root.join("person", JoinType.INNER);
            return PersonSpecifications.phoneEquals(person, cb, phone);
        };
    }

    public static Specification<Student> personEmailContains(String email) {
        return (root, query, cb) -> {
            Join<Student, Person> person = root.join("person", JoinType.INNER);
            return PersonSpecifications.emailContains(person, cb, email);
        };
    }

    public static Specification<Student> personDobGte(LocalDate from) {
        return (root, query, cb) -> {
            Join<Student, Person> person = root.join("person", JoinType.INNER);
            return PersonSpecifications.dobGte(person, cb, from);
        };
    }

    public static Specification<Student> personDobLte(LocalDate to) {
        return (root, query, cb) -> {
            Join<Student, Person> person = root.join("person", JoinType.INNER);
            return PersonSpecifications.dobLte(person, cb, to);
        };
    }

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
            return cb.like(cb.lower(major.get("name")), SpecUtils.likePattern(name));
        };
    }

    public static Specification<Student> studentCodeContains(String code) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(code)) return null;
            return cb.like(cb.lower(root.get("studentCode")), SpecUtils.likePattern(code));
        };
    }

    public static Specification<Student> enrollmentYearGte(Integer from) {
        return (root, query, cb) -> {
            if (from == null) return null;
            return cb.greaterThanOrEqualTo(root.get("enrollmentYear"), from);
        };
    }

    public static Specification<Student> enrollmentYearLte(Integer to) {
        return (root, query, cb) -> {
            if (to == null) return null;
            return cb.lessThanOrEqualTo(root.get("enrollmentYear"), to);
        };
    }
}
