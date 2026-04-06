package student.management.api_app.dto.auth;

public record AuthResponse(
        String token,
        String username,
        String role) {
}
