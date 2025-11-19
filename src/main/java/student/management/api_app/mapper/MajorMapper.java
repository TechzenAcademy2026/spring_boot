package student.management.api_app.mapper;

import org.springframework.stereotype.Component;
import student.management.api_app.dto.major.MajorDetailResponse;
import student.management.api_app.dto.major.MajorListItemResponse;
import student.management.api_app.model.Major;

@Component
public class MajorMapper {

    public MajorDetailResponse toDetailResponse(Major major) {
        return major == null ? null : new MajorDetailResponse(
                major.getId(),
                major.getCode(),
                major.getName(),
                major.getDescription(),
                major.getCreatedAt(),
                major.getUpdatedAt()
        );
    }

    public MajorListItemResponse toListItemResponse(Major major) {
        return new MajorListItemResponse(
                major.getId(),
                major.getCode(),
                major.getName()
        );
    }
}
