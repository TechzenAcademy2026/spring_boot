package student.management.api_app.service;

import org.springframework.data.domain.Pageable;
import student.management.api_app.dto.major.*;
import student.management.api_app.dto.page.PageResponse;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IMajorService {
    PageResponse<MajorListItemResponse> getAll(MajorSearchRequest req, Pageable pageable);

    List<MajorListItemResponse> listByIds(Collection<UUID> ids);

    MajorDetailResponse getById(UUID id);
    MajorDetailResponse getByCode(String code);
    MajorDetailResponse create(MajorCreateRequest req);

    MajorDetailResponse update(UUID id, MajorUpdateRequest req);

    void deleteById(UUID id);
}
