package student.management.api_app.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.major.MajorSearchRequest;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.dto.person.PersonCreateRequest;
import student.management.api_app.dto.person.PersonSearchRequest;
import student.management.api_app.dto.student.*;
import student.management.api_app.mapper.StudentMapper;
import student.management.api_app.model.Person;
import student.management.api_app.model.Student;
import student.management.api_app.repository.MajorRepository;
import student.management.api_app.repository.PersonRepository;
import student.management.api_app.repository.StudentRepository;
import student.management.api_app.service.IStudentService;
import student.management.api_app.util.AgeCalculator;

import java.util.UUID;

import static student.management.api_app.repository.specification.StudentSpecifications.*;
import static student.management.api_app.util.NormalizerUtil.*;

@Service
@RequiredArgsConstructor
public class StudentService implements IStudentService {
    private final StudentRepository studentRepo;
    private final PersonRepository personRepo;
    private final MajorRepository majorRepo;
    private final StudentMapper studentMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    @Override
    public PageResponse<StudentListItemResponse> getAll(Pageable pageable) {
        Page<StudentListItemResponse> pageData =
                studentRepo.findAllListItem(AgeCalculator.eighteenYearsAgo(), pageable);
        return new PageResponse<>(pageData);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<StudentListItemResponse> search(
            StudentSearchRequest req, Pageable pageable) {

        PersonSearchRequest pReq = req.person(); // Lưu ý có thể là null
        MajorSearchRequest mReq = req.major();

        String name = trimToNull(pReq != null ? pReq.name() : null);
        String phone = normalizePhone(pReq != null ? pReq.phone() : null);
        String email = normalizeEmail(pReq != null ? pReq.email() : null);
        String majorCode = normalizeCode(mReq != null ? mReq.code() : null);
        String majorName = trimToNull(mReq != null ? mReq.name() : null);
        String studentCode = normalizeCode(req.studentCode());

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

        Page<Student> pageData = studentRepo.findAll(spec, pageable);

        Page<StudentListItemResponse> mappedPageData =
                pageData.map(studentMapper::toListItemResponse);

        return new PageResponse<>(mappedPageData);
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<StudentListItemResponse> listByEnrollmentYear(
            Integer year, Pageable pageable) {
        Page<StudentListItemResponse> emptyPage = Page.empty(pageable);
        if (year == null) return new PageResponse<>(emptyPage);

        Page<Student> pageData = studentRepo.findByEnrollmentYear(year, pageable);
        Page<StudentListItemResponse> mappedPageData =
                pageData.map(studentMapper::toListItemResponse);

        return new PageResponse<>(mappedPageData);
    }

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

    @Transactional(readOnly = true)
    @Override
    public PageResponse<EnrollmentStatDTO> countStudentsGroupedByYear(Pageable pageable) {
        Page<EnrollmentStatDTO> pageData = studentRepo.countStudentsGroupedByYear(pageable);
        return new PageResponse<>(pageData);
    }

    @Transactional(readOnly = true)
    @Override
    public StudentDetailResponse getById(UUID id) {
        return studentRepo.findById(id)
                .map(studentMapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Override
    public StudentDetailResponse getByStudentCode(String studentCode) {
        String code = normalizeCode(studentCode);
        validateStudentCode(code);
        return studentRepo.findByStudentCode(code)
                .map(studentMapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found: " + studentCode));
    }

    @Transactional(readOnly = true)
    @Override
    public StudentDetailResponse getByPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized == null) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Phone is required");
        return studentRepo.findByPhone(normalized)
                .map(studentMapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Student not found with phone: " + phone));
    }

    @Transactional
    @Override
    public StudentDetailResponse create(StudentCreateRequest req) {
        if (req.person() == null || req.student() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "person and student are required");
        }

        // Tạo person trước
        PersonCreateRequest pReq = req.person();
        String fullName = trimToNull(pReq.fullName());
        String phone = normalizePhone(pReq.phone());
        String email = normalizeEmail(pReq.contactEmail());
        String address = trimToNull(pReq.address());

        validateFullName(fullName);
        checkExistedPhone(phone);

        Person p = Person.builder()
                .fullName(fullName)
                .dob(pReq.dob())
                .phone(phone)
                .contactEmail(email)
                .address(address)
                .build();

        try {
            personRepo.saveAndFlush(p);
            entityManager.refresh(p);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unique constraint violated in DB (Person)", e);
        }

        // Tạo student từ person vừa tạo này
        StudentCreateOnlyRequest sReq = req.student();
        String studentCode = normalizeCode(sReq.studentCode());

        validateStudentCode(studentCode);
        checkExistedStudentCode(studentCode);

        Student s = Student.builder()
                .person(p)
                .studentCode(studentCode)
                .enrollmentYear(sReq.enrollmentYear())
                .build();

        try {
            studentRepo.saveAndFlush(s);
            entityManager.refresh(s);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unique/FK constraint violated in DB (Student)", e);
        }

        return studentMapper.toDetailResponse(s);
    }

    @Transactional
    @Override
    public StudentDetailResponse createFromExistingPerson(StudentCreateFromPersonRequest req) {
        String studentCode = normalizeCode(req.studentCode());
        if (req.personId() == null || studentCode == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Person ID and student code are required");
        }

        Person p = personRepo.findById(req.personId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person not found with id: " + req.personId()));

        // Kiểm tra student này đã là student thì chặn (vì 1-1) -> ném 409
        checkExistedIdInStudent(req.personId());

        checkExistedStudentCode(studentCode);

        Student s = Student.builder()
                .person(p)
                .studentCode(studentCode)
                .enrollmentYear(req.enrollmentYear())
                .build();

        try {
            studentRepo.saveAndFlush(s);
            entityManager.refresh(s);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unique/FK constraint violated in DB (Student)", e);
        }

        return studentMapper.toDetailResponse(s);
    }

    @Transactional
    @Override
    public StudentDetailResponse patch(UUID id, StudentPatchRequest req) {
        Student s = studentRepo.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Student not found with id: " + id));

        // PATCH studentCode (không được phép null)
        if (req.studentCode().isPresent()) {
            String raw = req.studentCode().orElse(null);
            if (raw == null) throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Student code cannot be null");
            String newCode = normalizeCode(raw);
            if (newCode == null) throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Student code is required");
            if (!newCode.equals(s.getStudentCode())) {
                checkExistedStudentCode(newCode);
                s.setStudentCode(newCode);
            }
        }

        // PATCH enrollmentYear
        if (req.enrollmentYear().isPresent()) {
            s.setEnrollmentYear(req.enrollmentYear().orElse(null));
        }

        try {
            studentRepo.saveAndFlush(s);
            entityManager.refresh(s);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unique constraint violated in DB", e);
        }

        return studentMapper.toDetailResponse(s);
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        Student s = studentRepo.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Student not found with id: " + id));
        studentRepo.delete(s);
    }

    // ===== Helpers =====
    private void validateFullName(String fullName) {
        if (fullName == null) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Full name is required");
    }

    private void validateStudentCode(String studentCode) {
        if (studentCode == null) throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Student code is required");
    }

    private void checkExistedIdInStudent(UUID id) throws ResponseStatusException {
        if (studentRepo.existsById(id)) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "This person is already a student: " + id);
    }

    private void checkExistedPhone(String phone) throws ResponseStatusException {
        if (phone != null && personRepo.existsByPhone(phone)) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Phone " + phone + " is existed");
    }

    private void checkExistedStudentCode(String studentCode) throws ResponseStatusException {
        if (studentRepo.existsByStudentCode(studentCode)) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Student code " + studentCode + " is existed");
    }
}
