package student.management.api_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import student.management.api_app.model.Major;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MajorRepository extends
        JpaRepository<Major, UUID>, JpaSpecificationExecutor<Major> {

    Optional<Major> findByCode(String code);
    boolean existsByCode(String code);
}
