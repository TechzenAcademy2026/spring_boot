package student.management.api_app.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.person.PersonCreateRequest;
import student.management.api_app.dto.student.*;
import student.management.api_app.mapper.StudentMapper;
import student.management.api_app.model.Person;
import student.management.api_app.model.Student;
import student.management.api_app.repository.PersonRepository;
import student.management.api_app.repository.StudentRepository;
import student.management.api_app.service.IStudentService;

import java.util.List;
import java.util.UUID;

import static student.management.api_app.util.NormalizerUtil.*;

@Service
@RequiredArgsConstructor
public class StudentService implements IStudentService {
    private final StudentRepository studentRepo;
    private final PersonRepository personRepo;
    private final StudentMapper studentMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    @Override
    public List<StudentListItemResponse> getAll() {
        return studentRepo.findAll()
                .stream()
                .map(studentMapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<StudentListItemResponse> searchByPersonName(String keyword) {
        String kw = trimToNull(keyword);
        if (kw == null) return List.of();
        return studentRepo.findByPerson_FullNameContainingIgnoreCase(kw)
                .stream()
                .map(studentMapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<StudentListItemResponse> listByEnrollmentYear(Integer year) {
        if (year == null) return List.of();
        return studentRepo.findByEnrollmentYear(year)
                .stream()
                .map(studentMapper::toListItemResponse)
                .toList();
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

    private void checkExistedIdInStudent(UUID id) {
        if (studentRepo.existsById(id)) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "This person is already a student: " + id);
    }

    private void checkExistedPhone(String phone) {
        if (phone != null && personRepo.existsByPhone(phone)) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Phone " + phone + " is existed");
    }

    private void checkExistedStudentCode(String studentCode) {
        if (studentRepo.existsByStudentCode(studentCode)) throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Student code " + studentCode + " is existed");
    }
}
