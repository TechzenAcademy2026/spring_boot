package student.management.api_app.dto.major;

public record MajorCreateRequest(
        String code,
        String name,
        String description
) {
}
