package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.service.BrandService;
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

/** API quản lý thương hiệu. */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    /**
     * Tạo mới một thương hiệu.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BrandDto create(@Valid @RequestBody BrandDto request) {
        return brandService.create(request);
    }

    /**
     * Lấy chi tiết thương hiệu theo ID.
     */
    @GetMapping("/{id}")
    public BrandDto getById(@PathVariable UUID id) {
        return brandService.getById(id);
    }

    /**
     * Lấy danh sách thương hiệu với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    public Page<BrandDto> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return brandService.getList(keyword, status, pageable);
    }

    /**
     * Cập nhật thương hiệu theo ID.
     */
    @PutMapping("/{id}")
    public BrandDto update(@PathVariable UUID id, @Valid @RequestBody BrandDto request) {
        return brandService.update(id, request);
    }

    /**
     * Xóa thương hiệu theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        brandService.delete(id);
    }
}
