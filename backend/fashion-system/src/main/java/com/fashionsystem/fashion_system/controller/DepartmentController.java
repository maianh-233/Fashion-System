package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.service.DepartmentService;
import com.fashionsystem.fashion_system.service.PositionService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API quản lý phòng ban. */
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;
    private final PositionService positionService;

    /**
     * Tạo mới một phòng ban.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    public DepartmentDto create(@Valid @RequestBody DepartmentDto request) {
        return departmentService.create(request);
    }

    /**
     * Lấy chi tiết phòng ban theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public DepartmentDto getById(@PathVariable UUID id) {
        DepartmentDto detail = departmentService.getById(id);
        detail.setPositions(positionService.getByDepartment(id));
        return detail;
    }

    /**
     * Lấy danh sách phòng ban với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public Page<DepartmentDto> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return departmentService.getList(keyword, active, pageable);
    }

    /**
     * Cập nhật phòng ban theo ID.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    public DepartmentDto update(@PathVariable UUID id, @Valid @RequestBody DepartmentDto request) {
        return departmentService.update(id, request);
    }

    /**
     * Xóa phòng ban theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('DEPARTMENT_DELETE')")
    public void delete(@PathVariable UUID id) {
        departmentService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    public DepartmentDto restore(@PathVariable UUID id) {
        return departmentService.restore(id);
    }
}
