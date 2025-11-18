package student.management.api_app.dto.major;

import java.time.Instant;
import java.util.UUID;

public record MajorDetailResponse(
        UUID id,
        String code,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
