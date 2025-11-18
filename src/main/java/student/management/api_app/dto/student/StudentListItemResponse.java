package student.management.api_app.dto.student;

import java.util.UUID;

public record StudentListItemResponse(
        UUID id,
        String studentCode,
        Integer enrollmentYear,
        String fullName,
        String contactEmail,
        Boolean isAdult,
        String majorCode
) {
}
