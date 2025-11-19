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
import student.management.api_app.dto.person.PersonDetailResponse;
import student.management.api_app.dto.person.PersonListItemResponse;
import student.management.api_app.dto.person.PersonPatchRequest;
import student.management.api_app.mapper.PersonMapper;
import student.management.api_app.model.Person;
import student.management.api_app.repository.PersonRepository;
import student.management.api_app.service.IPersonService;
import student.management.api_app.util.NormalizerUtil;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService implements IPersonService {
    private final PersonRepository repo;
    private final PersonMapper mapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    @Override
    public List<PersonListItemResponse> getAll() {
        return repo.findAll()
                .stream()
                .map(mapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<PersonListItemResponse> searchByName(String keyword) {
        String kw = NormalizerUtil.trimToNull(keyword);
        if (kw == null) return List.of();
        return repo.findByFullNameContainingIgnoreCase(kw)
                .stream()
                .map(mapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<PersonListItemResponse> searchByContactEmail(String email) {
        String normalized = NormalizerUtil.normalizeEmail(email);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        return repo.findByContactEmailIgnoreCase(normalized)
                .stream()
                .map(mapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<PersonListItemResponse> listByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Set<UUID> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return repo.findAllById(distinctIds).stream()
                .map(mapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public PersonDetailResponse getById(UUID id) {
        return repo.findById(id)
                .map(mapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person not found with id: " + id
                ));
    }

    @Transactional(readOnly = true)
    @Override
    public PersonDetailResponse getByPhone(String phone) {
        String normalized = NormalizerUtil.normalizePhone(phone);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone is required");
        }

        return repo.findByPhone(normalized)
                .map(mapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person not found with phone: " + phone
                ));
    }

    @Transactional
    @Override
    public PersonDetailResponse create(PersonCreateRequest req) {
        String fullName = NormalizerUtil.trimToNull(req.fullName());
        String phone = NormalizerUtil.normalizePhone(req.phone());
        String email = NormalizerUtil.normalizeEmail(req.contactEmail());
        String address = NormalizerUtil.trimToNull(req.address());

        if (fullName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FullName is required");
        }
        if (phone != null) {
            if (repo.existsByPhone(phone)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Phone " + phone + " is existed");
            }
        }

        Person p = Person.builder()
                .fullName(fullName)
                .dob(req.dob())
                .phone(phone)
                .contactEmail(email)
                .address(address)
                .build();

        try {
            repo.saveAndFlush(p);
            entityManager.refresh(p);
        } catch (DataIntegrityViolationException e) {
            // Bắt race condition từ ràng buộc UNIQUE ở DB
            // khoảng giữa repo.existsByPhone() và save() vẫn có thể bị request khác làm thay đổi data
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unique constraint violated in DB", e);
        }

        return mapper.toDetailResponse(p);
    }

    @Transactional
    @Override
    public PersonDetailResponse patch(UUID id, PersonPatchRequest req) {
        Person p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person not found with id: " + id
                ));

        if (req.fullName().isPresent()) { // field "fullName" xuất hiện trong request JSON
            String raw = req.fullName()
                    .orElse(null); // request JSON {"fullName": null} thì .orElse(null) trả về null
            if (raw == null) { // Cấm xóa fullName (vì DB có ràng buộc NOT NULL)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FullName cannot be null");
            }
            String newValue = NormalizerUtil.trimToNull(raw);
            if (newValue == null) { // Field "fullName" của request JSON có value nhưng không hợp lệ (chỉ có space)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "FullName is required");
            }
            p.setFullName(newValue);
        }

        if (req.dob().isPresent()) {
            p.setDob(req.dob() // Có value thì set value đó
                    .orElse(null)); // Value là null thì set null -> xóa dob
        }


        if (req.phone().isPresent()) {
            String raw = req.phone().orElse(null);
            String newPhone = raw != null ? NormalizerUtil.normalizePhone(raw) : null;
            if (newPhone != null && !newPhone.equals(p.getPhone())) {
                if (repo.existsByPhone(newPhone)) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Phone " + newPhone + " is existed");
                }
            }
            p.setPhone(newPhone); // null => xóa phone
        }

        if (req.contactEmail().isPresent()) {
            String raw = req.contactEmail().orElse(null);
            String newEmail = raw != null ? NormalizerUtil.normalizeEmail(raw) : null;
            p.setContactEmail(newEmail); // null => xóa email
        }

        if (req.address().isPresent()) {
            String raw = req.address().orElse(null);
            String newAddress = raw != null ? NormalizerUtil.trimToNull(raw) : null;
            p.setAddress(newAddress); // null => xóa address
        }

        try {
            // save() của JPA là phương thức "upsert": vừa insert vừa update tùy trạng thái entity
            repo.saveAndFlush(p);
            entityManager.refresh(p);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unique constraint violated in DB", e);
        }

        return mapper.toDetailResponse(p);
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        Person p = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Person not found with id: " + id));

        repo.delete(p);
    }
}
