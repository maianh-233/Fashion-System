package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CollectionDto;
import com.fashionsystem.fashion_system.service.CollectionService;
import com.fashionsystem.fashion_system.security.AuthenticatedUser;
import com.fashionsystem.fashion_system.service.ProductAuthorizationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class CollectionController {
    private final CollectionService collectionService;
    private final ProductAuthorizationService productAuthorizationService;

    /**
     * Tạo mới một bộ sưu tập.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('COLLECTION_CREATE')")
    public CollectionDto create(Authentication authentication, @Valid @RequestBody CollectionDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "COLLECTION_CREATE");
        return collectionService.create(request);
    }

    /**
     * Lấy chi tiết bộ sưu tập theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COLLECTION_VIEW')")
    public CollectionDto getById(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireRead(userId(authentication), "COLLECTION_VIEW");
        return collectionService.getById(id);
    }

    /**
     * Lấy danh sách bộ sưu tập với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('COLLECTION_VIEW')")
    public Page<CollectionDto> getList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID brandId,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "COLLECTION_VIEW");
        return collectionService.getList(keyword, brandId, season, year, status, pageable);
    }

    /**
     * Cập nhật bộ sưu tập theo ID.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COLLECTION_UPDATE')")
    public CollectionDto update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody CollectionDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "COLLECTION_UPDATE");
        return collectionService.update(id, request);
    }

    /**
     * Xóa bộ sưu tập theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('COLLECTION_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "COLLECTION_DELETE");
        collectionService.delete(id);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
