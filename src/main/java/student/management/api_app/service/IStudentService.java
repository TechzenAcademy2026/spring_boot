package student.management.api_app.service;

import org.springframework.data.domain.Pageable;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.dto.student.*;

import java.util.List;
import java.util.UUID;

public interface IStudentService {
    PageResponse<StudentListItemResponse> getAll(Pageable pageable);
    PageResponse<StudentListItemResponse> search(StudentSearchRequest req, Pageable pageable);
    PageResponse<StudentListItemResponse> listByEnrollmentYear(Integer year, Pageable pageable);
    PageResponse<StudentListItemResponse> listByMajorId(UUID majorId, Pageable pageable);

    PageResponse<EnrollmentStatDTO> countStudentsGroupedByYear(Pageable pageable);

    StudentDetailResponse getById(UUID id);
    StudentDetailResponse getByStudentCode(String studentCode);
    StudentDetailResponse getByPhone(String phone);
    StudentDetailResponse create(StudentCreateRequest req);
    StudentDetailResponse createFromExistingPerson(StudentCreateFromPersonRequest req);
    StudentDetailResponse patch(UUID id, StudentPatchRequest req);

    void deleteById(UUID id);
}
