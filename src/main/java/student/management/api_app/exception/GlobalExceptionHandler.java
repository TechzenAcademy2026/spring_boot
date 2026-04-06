package student.management.api_app.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.AppResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

        // 1) Bắt lỗi Validation (MethodArgumentNotValidException)
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<AppResponse<Map<String, String>>> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                Map<String, String> fieldErrors = new HashMap<>();

                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

                log.warn("Validation failed for [{}]: {}", request.getRequestURI(), fieldErrors);

                return ResponseEntity.badRequest().body(
                                AppResponse.<Map<String, String>>builder()
                                                .success(false)
                                                .data(fieldErrors)
                                                .error(AppResponse.AppError.builder()
                                                                .code("VALIDATION_FAILED")
                                                                .message("Input validation failed")
                                                                .path(request.getRequestURI())
                                                                .build())
                                                .build());
        }

        // 2) Bắt ResourceNotFoundException
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<AppResponse<Void>> handleNotFound(
                        ResourceNotFoundException ex,
                        HttpServletRequest request) {

                log.warn("Resource not found: [{}] {}", request.getRequestURI(), ex.getMessage());

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                                AppResponse.<Void>builder()
                                                .success(false)
                                                .error(AppResponse.AppError.builder()
                                                                .code("NOT_FOUND")
                                                                .message(ex.getMessage())
                                                                .path(request.getRequestURI())
                                                                .build())
                                                .build());
        }

        // 3) Bắt lỗi đăng nhập sai (BadCredentialsException /
        // UsernameNotFoundException)
        @ExceptionHandler({
                        org.springframework.security.authentication.BadCredentialsException.class,
                        org.springframework.security.core.userdetails.UsernameNotFoundException.class
        })
        public ResponseEntity<AppResponse<Void>> handleBadCredentials(
                        Exception ex,
                        HttpServletRequest request) {

                log.warn("Login failed at [{}]: {}", request.getRequestURI(), ex.getMessage());

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                                AppResponse.<Void>builder()
                                                .success(false)
                                                .error(AppResponse.AppError.builder()
                                                                .code("LOGIN_FAILED")
                                                                .message("Sai username hoặc password")
                                                                .path(request.getRequestURI())
                                                                .build())
                                                .build());
        }

        // 4) Bắt lỗi phân quyền (AccessDeniedException từ @PreAuthorize)
        @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
        public ResponseEntity<AppResponse<Void>> handleAccessDenied(
                        org.springframework.security.access.AccessDeniedException ex,
                        HttpServletRequest request) {

                log.warn("Access denied at [{}]: {}", request.getRequestURI(), ex.getMessage());

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                                AppResponse.<Void>builder()
                                                .success(false)
                                                .error(AppResponse.AppError.builder()
                                                                .code("ACCESS_DENIED")
                                                                .message("Bạn không có quyền truy cập tài nguyên này")
                                                                .path(request.getRequestURI())
                                                                .build())
                                                .build());
        }

        // 5) Bắt ResponseStatusException (ném từ Service)
        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<AppResponse<Void>> handleResponseStatus(
                        ResponseStatusException ex,
                        HttpServletRequest request) {

                HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());

                log.warn("ResponseStatusException at [{}]: {} - {}", request.getRequestURI(), status, ex.getReason());

                return ResponseEntity.status(status).body(
                                AppResponse.<Void>builder()
                                                .success(false)
                                                .error(AppResponse.AppError.builder()
                                                                .code(status.name())
                                                                .message(ex.getReason())
                                                                .path(request.getRequestURI())
                                                                .build())
                                                .build());
        }

        // 4) Bắt mọi exception không mong đợi (fallback)
        @ExceptionHandler(Exception.class)
        public ResponseEntity<AppResponse<Void>> handleGeneral(
                        Exception ex,
                        HttpServletRequest request) {

                log.error("Unexpected error at [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                                AppResponse.<Void>builder()
                                                .success(false)
                                                .error(AppResponse.AppError.builder()
                                                                .code("INTERNAL_ERROR")
                                                                .message("An unexpected error occurred")
                                                                .path(request.getRequestURI())
                                                                .build())
                                                .build());
        }
}
