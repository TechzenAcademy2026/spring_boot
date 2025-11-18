package student.management.api_app.dto.student;

import student.management.api_app.dto.major.MajorDetailResponse;
import student.management.api_app.dto.person.PersonDetailResponse;

import java.time.Instant;

public record StudentDetailResponse(
        PersonDetailResponse personDetail,
        MajorDetailResponse majorDetail,

        String studentCode,
        Integer enrollmentYear,
        Instant createdAt,
        Instant updatedAt
) {
}
