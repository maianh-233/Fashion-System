package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.service.CategoryService;
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

/** API quản lý danh mục. */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    /**
     * Tạo mới một danh mục.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@Valid @RequestBody CategoryDto request) {
        return categoryService.create(request);
    }

    /**
     * Lấy chi tiết danh mục theo ID.
     */
    @GetMapping("/{id}")
    public CategoryDto getById(@PathVariable UUID id) {
        return categoryService.getById(id);
    }

    /**
     * Lấy danh sách danh mục với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    public Page<CategoryDto> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID parentId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return categoryService.getList(keyword, parentId, pageable);
    }

    /**
     * Cập nhật danh mục theo ID.
     */
    @PutMapping("/{id}")
    public CategoryDto update(@PathVariable UUID id, @Valid @RequestBody CategoryDto request) {
        return categoryService.update(id, request);
    }

    /**
     * Xóa danh mục theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
