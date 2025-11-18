package student.management.api_app.dto.major;

public record MajorUpdateRequest(
        String code,
        String name,
        String description
) {
}
