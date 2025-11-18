package student.management.api_app.controller.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import student.management.api_app.dto.AppResponse;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.dto.student.*;
import student.management.api_app.service.impl.StudentService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/students")
@Tag(name = "Student Management", description = "Student Management API")
@RequiredArgsConstructor
public class StudentController {
    @Value("${api.prefix}")
    private String apiPrefix;
    private final StudentService service;

    @Operation(
            summary = "Get all students with pagination",
            description = "Lấy danh sách tất cả học viên có phân trang",
            responses = @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping
    public ResponseEntity<AppResponse<PageResponse<StudentListItemResponse>>> getAll(
            @ParameterObject @PageableDefault(size = 5)
            Pageable pageable
    ) {
        return ResponseEntity.ok(AppResponse.success(service.getAll(pageable)));
    }

    @Operation(
            summary = "List students by enrollment year with pagination",
            description = "Lọc student theo enrollmentYear có phân trang",
            responses = @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("/by-year")
    public ResponseEntity<AppResponse<PageResponse<StudentListItemResponse>>> listByEnrollmentYear(
            @RequestParam("year") Integer year,
            @PageableDefault(size = 5, sort = "enrollmentYear", direction = Sort.Direction.DESC)
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(AppResponse.<PageResponse<StudentListItemResponse>>builder()
                .success(true)
                .data(service.listByEnrollmentYear(year, pageable))
                .build());
    }

    @Operation(
            summary = "List students by major id with pagination",
            description = "Lấy danh sách student theo id của chuyên ngành có phân trang",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @GetMapping("/by-major/{major_id}")
    public ResponseEntity<AppResponse<PageResponse<StudentListItemResponse>>> listStudentsByMajor(
            @PathVariable UUID major_id,
            @ParameterObject
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(AppResponse.success(service.listByMajorId(major_id, pageable)));
    }

    @Operation(
            summary = "Search students by attributes",
            description = """
                    Tìm kiếm học viên với nhiều điều kiện tùy chọn:
                    - name: chứa trong Person.fullName (ignore case)
                    - phone: đúng với Person.phone (sau normalize)
                    - email: chứa trong Person.contactEmail
                    - studentCode: chứa trong studentCode
                    - enrollmentYearFrom / enrollmentYearTo: khoảng năm nhập học
                    Hỗ trợ phân trang & sort theo mọi field hợp lệ (kể cả person.fullName)
                    """,
            responses = @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("/search")
    public ResponseEntity<AppResponse<PageResponse<StudentListItemResponse>>> search(
            @ParameterObject StudentSearchRequest req, // Để Swagger + Spring Doc hiểu khi bind từ query param
            @ParameterObject @PageableDefault(
                    size = 5, sort = {"createdAt", "person.fullName"}, direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(AppResponse.success(service.search(req, pageable)));
    }

    @Operation(
            summary = "Count students grouped by enrollment year",
            description = "Thống kê số lượng student theo enrollmentYear. " +
                    "Trả về danh sách DTO dạng (year, total)",
            responses = @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("stats/count-by-year")
    public ResponseEntity<AppResponse<PageResponse<EnrollmentStatDTO>>> countByEnrollmentYear(
            @ParameterObject
            @PageableDefault(size = 5, sort = "enrollmentYear")
            Pageable pageable) {
        return ResponseEntity.ok(AppResponse.success(service.countStudentsGroupedByYear(pageable)));
    }

    @Operation(
            summary = "Get student by id",
            description = "Lấy chi tiết học viên theo ID (trùng với personId)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "404", description = "Student not found",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class)))
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<AppResponse<StudentDetailResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.<StudentDetailResponse>builder()
                .success(true)
                .data(service.getById(id))
                .build());
    }

    @Operation(
            summary = "Get student by studentCode",
            description = "Tìm student theo studentCode",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "404", description = "Student not found",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class)))
            }
    )
    @GetMapping("/by-student-code")
    public ResponseEntity<AppResponse<StudentDetailResponse>> getByStudentCode(
            @RequestParam("student-code") String studentCode) {
        return ResponseEntity.ok(AppResponse.<StudentDetailResponse>builder()
                    .success(true)
                    .data(service.getByStudentCode(studentCode))
                .build());
    }

    @Operation(
            summary = "Get student by phone",
            description = "Tìm student theo số điện thoại. Trả về 404 nếu không tìm thấy",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "400", description = "Invalid phone",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "404", description = "Student not found",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @GetMapping("/by-phone")
    public ResponseEntity<AppResponse<StudentDetailResponse>> getByPhone(
            @RequestParam("phone") String phone) {
        return ResponseEntity.ok(AppResponse.<StudentDetailResponse>builder()
                .success(true)
                .data(service.getByPhone(phone))
                .build());
    }

    @Operation(
            summary = "Create student with new person (composite create)",
            description = """
                    Tạo mới Person và Student trong cùng một transaction.
                    Trả về 201 Created và Location header. Body gồm:
                    - person: thông tin cá nhân (fullName bắt buộc, phone unique nếu có)
                    - student: thông tin sinh viên (studentCode bắt buộc, unique)
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "409", description = "Unique constraint",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class)))
            }
    )
    @PostMapping
    public ResponseEntity<AppResponse<StudentDetailResponse>> create(
            @RequestBody StudentCreateRequest req) {
        StudentDetailResponse created = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.personDetail().id())
                .toUri();

        return ResponseEntity.created(location).body(AppResponse.<StudentDetailResponse>builder()
                .success(true)
                .data(created)
                .build());
    }

    @Operation(
            summary = "Create student from existing person",
            description = "Tạo Student cho Person đã tồn tại. Trả về 201 Created và location header",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Created"),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "404", description = "Person not found",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "409",
                            description = "Student already exists for person / studentCode duplicate",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class))),
            }
    )
    @PostMapping("/by-person")
    public ResponseEntity<AppResponse<StudentDetailResponse>> createFromExistingPerson(
            @RequestBody StudentCreateFromPersonRequest req) {
        StudentDetailResponse created = service.createFromExistingPerson(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath(apiPrefix + "/students/{id}")
                .buildAndExpand(created.personDetail().id())
                .toUri();

        return ResponseEntity.created(location).body(AppResponse.<StudentDetailResponse>builder()
                .success(true)
                .data(created)
                .build());
    }

    @Operation(
            summary = "Patch student by ID",
            description = "Cập nhật từng phần cho học viên. " +
                    "Truyền field cần cập nhật, gửi null để xóa field (nếu cho phép)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "404", description = "Student not found",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "409",
                            description = "Unique constraint violated in DB",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class)))
            }
    )
    @PatchMapping("/{id}")
    public ResponseEntity<AppResponse<StudentDetailResponse>> patch(
            @PathVariable UUID id,
            @RequestBody StudentPatchRequest req) {
        return ResponseEntity.ok(AppResponse.<StudentDetailResponse>builder()
                .success(true)
                .data(service.patch(id, req))
                .build());
    }

    @Operation(
            summary = "Delete student by ID",
            description = "Xóa học viên theo ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No content"),
                    @ApiResponse(responseCode = "404", description = "Student not found",
                            content = @Content(schema = @Schema(
                                    implementation = AppResponse.AppError.class)))
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}