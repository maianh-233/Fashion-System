package com.fashionsystem.fashion_system.controller;

import com.fashionsystem.fashion_system.dto.CategoryDto;
import com.fashionsystem.fashion_system.service.CategoryService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
@PreAuthorize("principal instanceof T(com.fashionsystem.fashion_system.security.AuthenticatedUser)")
public class CategoryController {
    private final CategoryService categoryService;
    private final ProductAuthorizationService productAuthorizationService;

    /**
     * Tạo mới một danh mục.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CATEGORY_CREATE')")
    public CategoryDto create(Authentication authentication, @Valid @RequestBody CategoryDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "CATEGORY_CREATE");
        return categoryService.create(request);
    }

    /**
     * Lấy chi tiết danh mục theo ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public CategoryDto getById(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireRead(userId(authentication), "CATEGORY_VIEW");
        CategoryDto category = categoryService.getById(id);
        productAuthorizationService.requireActiveForStore(userId(authentication), Boolean.TRUE.equals(category.getActive()));
        return category;
    }

    /**
     * Lấy danh sách danh mục với tìm kiếm, lọc, sắp xếp và phân trang.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public Page<CategoryDto> getList(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UUID parentId,
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        productAuthorizationService.requireRead(userId(authentication), "CATEGORY_VIEW");
        return categoryService.getList(keyword, parentId,
                productAuthorizationService.visibleActive(userId(authentication), active), pageable);
    }

    /**
     * Cập nhật danh mục theo ID.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public CategoryDto update(Authentication authentication, @PathVariable UUID id,
            @Valid @RequestBody CategoryDto request) {
        productAuthorizationService.requireMutation(userId(authentication), "CATEGORY_UPDATE");
        return categoryService.update(id, request);
    }

    /**
     * Xóa danh mục theo ID.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CATEGORY_DELETE')")
    public void delete(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "CATEGORY_DELETE");
        categoryService.delete(id);
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('CATEGORY_UPDATE')")
    public CategoryDto restore(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireMutation(userId(authentication), "CATEGORY_UPDATE");
        return categoryService.restore(id);
    }

    @GetMapping("/{id}/impact")
    @PreAuthorize("hasAuthority('CATEGORY_VIEW')")
    public java.util.Map<String, Long> impact(Authentication authentication, @PathVariable UUID id) {
        productAuthorizationService.requireRead(userId(authentication), "CATEGORY_VIEW");
        return java.util.Map.of("products", categoryService.affectedProducts(id),
                "categories", categoryService.affectedChildren(id));
    }

    private UUID userId(Authentication authentication) {
        return ((AuthenticatedUser) authentication.getPrincipal()).userId();
    }
}
