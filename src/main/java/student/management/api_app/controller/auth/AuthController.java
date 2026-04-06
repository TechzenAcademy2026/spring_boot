package student.management.api_app.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import student.management.api_app.dto.auth.AuthRequest;
import student.management.api_app.dto.auth.AuthResponse;
import student.management.api_app.util.JwtUtil;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "API xác thực và đăng nhập")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Operation(summary = "Login", description = "Đăng nhập bằng username/password, trả về JWT token", responses = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công"),
            @ApiResponse(responseCode = "401", description = "Sai username hoặc password")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {

        log.info("Login attempt for user: {}", request.username());

        // 1. Xác thực username & password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        // 2. Lấy UserDetails sau khi xác thực thành công
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 3. Tạo JWT Token
        String token = jwtUtil.generateToken(userDetails);

        // 4. Trả về token + thông tin user
        String role = userDetails.getAuthorities().iterator().next().getAuthority();

        log.info("Login successful for user: {}, role: {}", userDetails.getUsername(), role);
        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername(), role));
    }
}
