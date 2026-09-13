package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.BrandDto;
import com.fashionsystem.fashion_system.service.BrandService;
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

/** API quản lý thương hiệu. */
@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class BrandController {
    private final BrandService brandService;
    private final ProductAuthorizationService productAuthorizationService;

    /**
     * Tạo mới một thương hiệu.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('BRAND_CREATE')")
    public BrandDto create(Authentication authentication, @Valid @RequestBody BrandDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "BRAND_CREATE");
        return brandService.create(request);
    }

    /**
     * Lấy chi tiết thương hiệu theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_VIEW')")
    public BrandDto getById(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireRead(userId(authentication), "BRAND_VIEW");
        return brandService.getById(id);
    }

    /**
     * Lấy danh sách thương hiệu với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('BRAND_VIEW')")
    public Page<BrandDto> getList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "BRAND_VIEW");
        return brandService.getList(keyword, status, pageable);
    }

    /**
     * Cập nhật thương hiệu theo ID.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_UPDATE')")
    public BrandDto update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody BrandDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "BRAND_UPDATE");
        return brandService.update(id, request);
    }

    /**
     * Xóa thương hiệu theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "BRAND_DELETE");
        brandService.delete(id);
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
