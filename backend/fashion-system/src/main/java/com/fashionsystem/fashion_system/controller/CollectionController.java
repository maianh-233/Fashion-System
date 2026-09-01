package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.service.CollectionService;
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

/** API quản lý bộ sưu tập. */
@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {
    private final CollectionService collectionService;

    /**
     * Tạo mới một bộ sưu tập.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CollectionDto create(@Valid @RequestBody CollectionDto request) {
        return collectionService.create(request);
    }

    /**
     * Lấy chi tiết bộ sưu tập theo ID.
     */
    @GetMapping("/{id}")
    public CollectionDto getById(@PathVariable UUID id) {
        return collectionService.getById(id);
    }

    /**
     * Lấy danh sách bộ sưu tập với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    public Page<CollectionDto> getList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return collectionService.getList(keyword, brandId, season, year, status, pageable);
    }

    /**
     * Cập nhật bộ sưu tập theo ID.
     */
    @PutMapping("/{id}")
    public CollectionDto update(@PathVariable UUID id, @Valid @RequestBody CollectionDto request) {
        return collectionService.update(id, request);
    }

    /**
     * Xóa bộ sưu tập theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        collectionService.delete(id);
    }
}
