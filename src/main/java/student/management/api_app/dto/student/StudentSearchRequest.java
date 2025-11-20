package student.management.api_app.dto.student;

import student.management.api_app.dto.major.MajorSearchRequest;
import student.management.api_app.dto.person.PersonSearchRequest;

public record StudentSearchRequest(

        PersonSearchRequest person,
        MajorSearchRequest major,

        String studentCode,
        Integer enrollmentYearFrom,
        Integer enrollmentYearTo
) {}
