package student.management.api_app.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.major.*;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.mapper.MajorMapper;
import student.management.api_app.model.Major;
import student.management.api_app.repository.MajorRepository;
import student.management.api_app.repository.specification.MajorSpecifications;
import student.management.api_app.service.IMajorService;
import student.management.api_app.util.IdUtils;
import student.management.api_app.util.NormalizerUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MajorService implements IMajorService {

    private final MajorRepository repo;
    private final MajorMapper majorMapper;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    @Override
    public PageResponse<MajorListItemResponse> getAll(MajorSearchRequest req, Pageable pageable) {
        String code = NormalizerUtil.trimToNull(req.code());
        String name = NormalizerUtil.trimToNull(req.name());

        Specification<Major> spec = Specification.<Major>unrestricted()
                .and(MajorSpecifications.nameContains(name))
                .and(MajorSpecifications.codeContains(code));

        Page<MajorListItemResponse> pageData =
                repo.findAll(spec, pageable).map(majorMapper::toListItemResponse);

        return new PageResponse<>(pageData);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MajorListItemResponse> listByIds(Collection<UUID> ids) {
        Set<UUID> distinctIds = IdUtils.distinctNonNull(ids);
        return distinctIds.isEmpty() ? List.of() : repo.findAllById(distinctIds).stream()
                .map(majorMapper::toListItemResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public MajorDetailResponse getById(UUID id) {
        return repo.findById(id)
                .map(majorMapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Major not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public MajorDetailResponse getByCode(String code) {
        String normalized = NormalizerUtil.normalizeCode(code);
        validateCode(normalized);

        return repo.findByCode(normalized)
                .map(majorMapper::toDetailResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Major not found with major code: " + normalized));
    }

    @Transactional
    @Override
    public MajorDetailResponse create(MajorCreateRequest req) {
        String code = NormalizerUtil.normalizeCode(req.code());
        String name = NormalizerUtil.trimToNull(req.name());
        String description = NormalizerUtil.trimToNull(req.description());

        validateCode(code);
        validateName(name);

        checkExistedCode(code);

        Major m = Major.builder()
                .code(code)
                .name(name)
                .description(description)
                .build();

        try {
            repo.saveAndFlush(m);
            entityManager.refresh(m);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Unique constraint violated in DB", e);
        }

        return majorMapper.toDetailResponse(m);
    }

    // PUT method
    @Transactional
    @Override
    public MajorDetailResponse update(UUID id, MajorUpdateRequest req) {
        String code = NormalizerUtil.normalizeCode(req.code());
        String name = NormalizerUtil.trimToNull(req.name());
        String description = NormalizerUtil.trimToNull(req.description());

        validateCode(code);
        validateName(name);

        Major m = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Major not found with id: " + id));

        if (!m.getCode().equals(code)) checkExistedCode(code);

        m.setCode(code);
        m.setName(name);
        m.setDescription(description);

        try {
            repo.saveAndFlush(m);
            entityManager.refresh(m);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unique constraint violated in DB", e);
        }

        return majorMapper.toDetailResponse(m);
    }

    @Transactional
    @Override
    public void deleteById(UUID id) {
        Major m = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Major not found with id: " + id));

        try {
            repo.delete(m);
            repo.flush(); // để FK violation nổ ngay trong transaction này
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete major because it is referenced by other entities",
                    e
            );
        }
    }


    // ===== Helpers =====
    private void validateCode(String code) throws ResponseStatusException {
        if (code == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Major code is required");
        }
    }

    private void validateName(String name) throws ResponseStatusException {
        if (name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Major name is required");
        }
    }

    private void checkExistedCode(String code) throws ResponseStatusException {
        if (repo.existsByCode(code)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Major code " + code + " already existed");
        }
    }
}
