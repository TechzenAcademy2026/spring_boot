package student.management.api_app.dto.major;

import java.util.UUID;

public record MajorListItemResponse(
        UUID id,
        String code,
        String name
) {
}
