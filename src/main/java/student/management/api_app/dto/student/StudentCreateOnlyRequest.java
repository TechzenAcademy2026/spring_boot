package student.management.api_app.dto.student;

import java.util.UUID;

public record StudentCreateOnlyRequest(
        String studentCode,
        Integer enrollmentYear,
        UUID majorId
) {
}
