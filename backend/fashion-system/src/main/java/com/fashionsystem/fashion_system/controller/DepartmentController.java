package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.DepartmentDto;
import com.fashionsystem.fashion_system.service.DepartmentService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /**
     * Tạo mới một phòng ban.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DepartmentDto create(@Valid @RequestBody DepartmentDto request) {
        return departmentService.create(request);
    }

    /**
     * Lấy chi tiết phòng ban theo ID.
     */
    @GetMapping("/{id}")
    public DepartmentDto getById(@PathVariable UUID id) {
        return departmentService.getById(id);
    }

    /**
     * Lấy danh sách phòng ban với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
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
    public DepartmentDto update(@PathVariable UUID id, @Valid @RequestBody DepartmentDto request) {
        return departmentService.update(id, request);
    }

    /**
     * Xóa phòng ban theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        departmentService.delete(id);
    }
}
