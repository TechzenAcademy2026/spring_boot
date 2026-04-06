package student.management.api_app.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank(message = "Username không được để trống") String username,

        @NotBlank(message = "Password không được để trống") String password) {
}
