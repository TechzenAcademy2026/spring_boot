package student.management.api_app.controller.major;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import student.management.api_app.dto.AppResponse;
import student.management.api_app.dto.major.*;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.service.impl.MajorService;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService service;

    @Operation(
            summary = "Get all majors with pagination and search by attributes",
            description = "Lấy danh sách tất cả học viên có phân trang và tìm kiếm theo các thuộc tính",
            responses = @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping
    public ResponseEntity<AppResponse<PageResponse<MajorListItemResponse>>> getAll(
            @ParameterObject MajorSearchRequest req,
            @ParameterObject
            @PageableDefault(size = 5, sort = {"createdAt", "name"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(AppResponse.success(service.getAll(req, pageable)));
    }

    @Operation(
            summary = "Get major detail by id",
            description = "Lấy chi tiết chuyên ngành theo ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "404", description = "Major not found",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<AppResponse<MajorDetailResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(AppResponse.success(service.getById(id)));
    }

    @Operation(
            summary = "Get major detail by major code",
            description = "Lấy chi tiết chuyên ngành theo major code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "400", description = "Invalid major code",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "404", description = "Major not found",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @GetMapping("/by-major-code")
    public ResponseEntity<AppResponse<MajorDetailResponse>> getByCode(
            @RequestParam(name = "major-code") String code) {
        return ResponseEntity.ok(AppResponse.success(service.getByCode(code)));
    }

    @Operation(
            summary = "Get bulk major by multiple ids",
            description = "Nhận danh sách UUID qua body (POST) để tránh giới hạn độ dài URL." +
                    "Trả về danh sách rỗng nếu danh sách UUID trống," +
                    "hoặc tìm không thấy UUID nào khớp",
            responses = @ApiResponse(responseCode = "200", description = "Success")
    )
    @PostMapping("/bulk-by-ids")
    public ResponseEntity<AppResponse<List<MajorListItemResponse>>> bulkByIds(
            @RequestBody Collection<UUID> ids) {
        return ResponseEntity.ok(AppResponse.success(service.listByIds(ids)));
    }

    @Operation(
            summary = "Create a new major",
            description = "Tạo chuyên ngành mới. Trả về 201 Created và Location header",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Success"),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "409", description = "Unique constraint violated",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @PostMapping
    public ResponseEntity<AppResponse<MajorDetailResponse>> create(
            @RequestBody MajorCreateRequest req) {
        MajorDetailResponse created = service.create(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(AppResponse.success(created));
    }

    @Operation(
            summary = "Update major by ID",
            description = "Cập nhật cho major theo ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success"),
                    @ApiResponse(responseCode = "400", description = "Invalid input",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "409", description = "Unique constraint violated",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @PutMapping("/{id}")
    public ResponseEntity<AppResponse<MajorDetailResponse>> update(
            @PathVariable UUID id,
            @RequestBody MajorUpdateRequest req) {
        return ResponseEntity.ok(AppResponse.success(service.update(id, req)));
    }

    @Operation(
            summary = "Delete major by ID",
            description = "Xóa chuyên ngành theo ID",
            responses = {
                    @ApiResponse(responseCode = "204", description = "No content"),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class))),
                    @ApiResponse(responseCode = "409", description = "Conflict",
                            content = @Content(schema = @Schema(implementation = AppResponse.AppError.class)))
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
